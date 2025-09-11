package sizz.api.news.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sizz.api.news.dto.GeminiRequest;
import sizz.api.news.dto.GeminiResponse;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiAPI {

    private final WebClient geminiWebClient;

    @Value("${gemini.apiKey}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.maxOutputTokens.summaryAndInclination}")
    private int maxTokensSummaryAndInclination;

    private static final long BASE_BACKOFF_MS = 800;      // 지수 백오프 시작
    private static final long MAX_BACKOFF_MS  = 8000;     // 백오프 상한
    private static final long JITTER_MS       = 300;      // 지터(무작위)
    private static final int  MAX_RETRIES     = 3;

    // JSON 파싱기
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 429/5xx 재시도성 예외 */
    private static class RetryableException extends RuntimeException {
        final long nextDelayMs;
        RetryableException(String msg, long nextDelayMs) {
            super(msg);
            this.nextDelayMs = nextDelayMs;
        }
    }

    /** 단건 호출 (실패 시 예외 발생) */
    private String callOnce(String prompt, int maxTokens) {
        GeminiRequest req = GeminiRequest.of(prompt, maxTokens);

        GeminiResponse response = geminiWebClient.post()
                .uri(b -> b.path("/v1beta/models/" + model + ":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> handleError(r)) // 4xx/5xx 공통 처리
                .bodyToMono(GeminiResponse.class)
                .block();

        String raw = Optional.ofNullable(response)
                .map(GeminiResponse::getCandidates)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(GeminiResponse.Candidate::getContent)
                .map(GeminiResponse.Content::getParts)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(GeminiResponse.Part::getText)
                .orElse("");

        log.info("[Gemini] Raw response: {}", safeToJson(response));
        return extractJson(raw == null ? "" : raw.trim());
    }

    /** 오류 처리: 429는 Retry-After 존중, 5xx는 지수 백오프 권장 */
    private Mono<? extends Throwable> handleError(ClientResponse r) {
        return r.bodyToMono(String.class).defaultIfEmpty("")
                .flatMap(body -> {
                    HttpStatusCode code = r.statusCode();
                    // Retry-After 헤더(초)를 읽어 다음 지연으로 사용
                    long retryAfterMs = r.headers().header(HttpHeaders.RETRY_AFTER).stream()
                            .findFirst()
                            .map(s -> {
                                try { return Long.parseLong(s.trim()) * 1000L; }
                                catch (Exception ignored) { return 0L; }
                            })
                            .orElse(0L);

                    log.error("[Gemini] HTTP {}: {}", code, body);

                    int series = code.value() / 100;
                    if (code.value() == 429 || series == 5) {
                        long base = retryAfterMs > 0 ? retryAfterMs : BASE_BACKOFF_MS;
                        // 즉시 재시도가 아니라 상위에서 백오프 후 재시도하도록 RetryableException 던짐
                        return Mono.error(new RetryableException(
                                "Retryable " + code + " from Gemini", base
                        ));
                    }
                    return Mono.error(new IllegalStateException("Gemini API error " + code));
                });
    }

    /** 지수 백오프 + Jitter + Retry-After 기반 재시도 */
    private Optional<String> callGeminiWithRetry(String prompt, int maxTokens, int maxRetries) {
        long nextDelay = 0; // 첫 시도는 즉시
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (nextDelay > 0) {
                log.info("Gemini 재시도 {}회차 - {}ms 후 재시도", attempt, nextDelay);
                sleepSilently(nextDelay);
            }
            try {
                String text = callOnce(prompt, maxTokens);
                if (text != null && !text.isBlank()) {
                    return Optional.of(text);
                }
                // 빈 응답이면 재시도(드물지만 보호)
                throw new RetryableException("Empty body from Gemini", BASE_BACKOFF_MS);
            } catch (RetryableException re) {
                // Retry-After를 최우선으로, 없으면 지수 백오프 적용
                long jitter = ThreadLocalRandom.current().nextLong(0, JITTER_MS + 1);
                long backoff = (long) Math.min(MAX_BACKOFF_MS,
                        (re.nextDelayMs > 0 ? re.nextDelayMs : (BASE_BACKOFF_MS * Math.pow(2, attempt - 1))))
                        + jitter;
                nextDelay = backoff;
                if (attempt == maxRetries) {
                    log.warn("Gemini 재시도 한도 초과: {}", re.getMessage());
                    return Optional.empty();
                }
            } catch (Exception e) {
                // 비재시도성 예외
                log.error("[Gemini] 호출/파싱 실패(비재시도): {}", e.getMessage(), e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    /** 모델이 ```json ... ``` 포맷을 줄 수도 있어 JSON 객체만 추출 */
    private static String extractJson(String raw) {
        if (raw == null) return null;
        String s = raw.replace("```json", "").replace("```", "").trim();
        int st = s.indexOf('{'), ed = s.lastIndexOf('}');
        return (st >= 0 && ed > st) ? s.substring(st, ed + 1).trim() : s;
    }

    private String safeToJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return String.valueOf(o); }
    }

    // 퍼블릭 API

    /** 뉴스 요약 및 성향 분석 */
    @RateLimiter(name = "geminiApi")
    public Optional<SummaryAndInclination> summarizeAndIncline(String description) {
        String prompt =
                "당신은 한국어 뉴스 편집자입니다. 아래 텍스트를 보고 JSON 한 줄만 출력하세요.\n" +
                        "- 원문을 그대로 반복하지 말고, 의미를 3~5문장으로 압축 요약하세요.\n" +
                        "- 원문이 불완전하거나 짧아도 확인 가능한 사실만 요약하세요.\n" +
                        "- 새로운 사실이나 추측은 절대 추가하지 마세요.\n" +
                        "- 요약 길이는 300~400자 이내.\n" +
                        "- 성향은 '진보' | '중립' | '보수' 중 하나만 선택(애매하면 '중립').\n" +
                        "- 반드시 JSON 한 줄만 출력하세요. 코드블럭, 설명 금지.\n" +
                        "출력 예시: {\"summary\":\"...\",\"inclination\":\"중립\"}\n\n" +
                        "뉴스 본문:\n" + description;

        return callGeminiWithRetry(prompt, maxTokensSummaryAndInclination, MAX_RETRIES)
                .flatMap(this::parseSummaryAndInclination);
    }

    // JSON 파싱

    private Optional<SummaryAndInclination> parseSummaryAndInclination(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        try {
            JsonNode node = objectMapper.readTree(text);
            String summary = safeText(node, "summary");
            String inc = safeText(node, "inclination");

            if (summary == null || summary.isBlank()) summary = "요약 실패";

            String normalized = normalizeInclination(inc);
            if (!normalized.matches("진보|중립|보수")) {
                normalized = "분석 실패";
            }
            return Optional.of(new SummaryAndInclination(summary, normalized));
        } catch (Exception e) {
            log.warn("Gemini JSON 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String safeText(JsonNode node, String field) {
        return (node != null && node.has(field) && node.get(field).isTextual())
                ? node.get(field).asText()
                : null;
    }

    private static String normalizeInclination(String inc) {
        if (inc == null) return "";
        return inc.toLowerCase(Locale.ROOT)
                .replace("progressive", "진보")
                .replace("conservative", "보수")
                .replace("neutral", "중립")
                .replaceAll("[^가-힣]", "")
                .replaceAll(".*(진보|중립|보수).*", "$1")
                .trim();
    }

    // 결과 전달용 DTO
    public record SummaryAndInclination(String summary, String inclination) {}
}