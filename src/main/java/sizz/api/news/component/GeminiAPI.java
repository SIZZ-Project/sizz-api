package sizz.api.news.component;

import com.fasterxml.jackson.core.type.TypeReference;
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

import java.util.List;
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

    @Value("${gemini.maxOutputTokens.insightKeywords}")
    private int maxTokensInsightKeywords;

    private static final long BASE_BACKOFF_MS = 800;      // 지수 백오프 시작
    private static final long MAX_BACKOFF_MS  = 15000;    // 백오프 상한
    private static final long JITTER_MS       = 300;      // 지터(무작위)
    private static final int  MAX_RETRIES     = 3;

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
    private String callOnce(String prompt, String ctx) {
        GeminiRequest req =
                (ctx != null && ctx.startsWith("keywords:"))
                        ? GeminiRequest.ofKeywords(prompt, maxTokensInsightKeywords)   // 키워드 스키마
                        : GeminiRequest.ofSummary(prompt, maxTokensSummaryAndInclination); // 요약 스키마

        // 요청 바디 로깅
        try {
            log.info("[Gemini] ({}) request={}", ctx, objectMapper.writeValueAsString(req));
        } catch (Exception e) {
            log.warn("[Gemini] ({}) request 직렬화 실패: {}", ctx, e.getMessage());
        }

        GeminiResponse response = geminiWebClient.post()
                .uri(b -> b.path("/v1beta/models/" + model + ":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> handleError(r))
                .bodyToMono(GeminiResponse.class)
                .block();

        log.info("[Gemini] ({}) Raw response: {}", ctx, safeToJson(response));

        var parts = Optional.ofNullable(response)
                .map(GeminiResponse::getCandidates)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(GeminiResponse.Candidate::getContent)
                .map(GeminiResponse.Content::getParts)
                .orElse(null);

        if (parts == null || parts.isEmpty()) {
            log.warn("[Gemini] ({}) 빈 본문 응답(parts:null or empty)", ctx);
            return "";
        }

        String raw = Optional.of(parts)
                .map(l -> l.get(0))
                .map(GeminiResponse.Part::getText)
                .orElse("");

        String extracted = extractJson(raw == null ? "" : raw.trim());
        if (extracted.isBlank()) {
            log.warn("[Gemini] ({}) extractJson 결과가 빈 문자열", ctx);
        }
        return extracted;
    }

    /** 오류 처리 */
    private Mono<? extends Throwable> handleError(ClientResponse r) {
        return r.bodyToMono(String.class).defaultIfEmpty("")
                .flatMap(body -> {
                    HttpStatusCode code = r.statusCode();
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
                        return Mono.error(new RetryableException(
                                "Retryable " + code + " from Gemini", base
                        ));
                    }
                    return Mono.error(new IllegalStateException("Gemini API error " + code));
                });
    }

    /** 지수 백오프 + Jitter + Retry-After 기반 재시도 */
    private Optional<String> callGeminiWithRetry(String prompt, int maxRetries, String ctx) {
        long nextDelay = 0;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (nextDelay > 0) {
                log.info("[Gemini] ({}) 재시도 {}회차 - {}ms 후 재시도", ctx, attempt, nextDelay);
                sleepSilently(nextDelay);
            }
            try {
                String text = callOnce(prompt, ctx);
                if (text != null && !text.isBlank()) {
                    return Optional.of(text);
                }
                throw new RetryableException("Empty body from Gemini", BASE_BACKOFF_MS);
            } catch (RetryableException re) {
                long jitter = ThreadLocalRandom.current().nextLong(0, JITTER_MS + 1);
                long backoff = (long) Math.min(MAX_BACKOFF_MS,
                        (re.nextDelayMs > 0 ? re.nextDelayMs : (BASE_BACKOFF_MS * Math.pow(2, attempt - 1))))
                        + jitter;
                nextDelay = backoff;
                if (attempt == maxRetries) {
                    log.warn("[Gemini] ({}) 재시도 한도 초과: {}", ctx, re.getMessage());
                    return Optional.empty();
                }
            } catch (Exception e) {
                log.error("[Gemini] ({}) 호출/파싱 실패(비재시도): {}", ctx, e.getMessage(), e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    /** 모델이 ```json ... ``` 포맷을 줄 수도 있어 JSON 객체/배열만 추출 */
    private static String extractJson(String raw) {
        if (raw == null) return null;
        String s = raw.replace("```json", "").replace("```", "").trim();

        int objSt = s.indexOf('{'), objEd = s.lastIndexOf('}');
        if (objSt >= 0 && objEd > objSt) {
            return s.substring(objSt, objEd + 1).trim();
        }

        int arrSt = s.indexOf('['), arrEd = s.lastIndexOf(']');
        if (arrSt >= 0 && arrEd > arrSt) {
            return s.substring(arrSt, arrEd + 1).trim();
        }
        return s;
    }

    private String safeToJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return String.valueOf(o); }
    }

    private static String sample(String s) {
        if (s == null) return "null";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...(+" + (s.length() - 200) + ")";
    }

    // 퍼블릭 API

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

        return callGeminiWithRetry(prompt, MAX_RETRIES, "summary")
                .flatMap(this::parseSummaryAndInclination);
    }

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
            log.warn("Gemini JSON 파싱 실패(summary): {} / sample={}", e.getMessage(), sample(text));
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

    public record SummaryAndInclination(String summary, String inclination) {}

    @RateLimiter(name = "geminiApi")
    public Optional<List<String>> generateKeywords(String field) {
        String prompt =
                "아래 형식의 JSON 배열만 출력하세요. 한국어로.\n" +
                        "- 주제: 최근 주요 " + field + " 분야 이슈 키워드\n" +
                        "- 항목 수: 6~8개\n" +
                        "- 각 항목은 정확히 \"키워드: 설명\" 형식의 문자열 하나\n" +
                        "- 설명은 한 문장, 40~60글자, 불릿/번호/마크다운/코드블럭 금지\n" +
                        "- 예시: [\"금리인하: 기준금리 인하 기대가 금융시장에 파급되고 있다.\", " +
                        "\"환율: 달러 강세로 원화 변동성이 확대되고 있다.\"]\n" +
                        "- JSON 외 어떤 텍스트도 출력 금지";

        String ctx = "keywords:" + field;
        return callGeminiWithRetry(prompt, MAX_RETRIES, ctx)
                .flatMap(this::parseKeywords);
    }

    private Optional<List<String>> parseKeywords(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(text, new TypeReference<List<String>>(){}));
        } catch (Exception e) {
            log.warn("Gemini 키워드 파싱 실패: {} / sample={}", e.getMessage(), sample(text));
            return Optional.empty();
        }
    }
}