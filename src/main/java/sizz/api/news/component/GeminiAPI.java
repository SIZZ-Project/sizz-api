package sizz.api.news.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import sizz.api.news.dto.GeminiRequest;
import sizz.api.news.dto.GeminiResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    // JSON 파싱기
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 공통 호출
    private Optional<String> callGeminiAndExtractText(String prompt, int maxTokens) {
        GeminiRequest geminiRequest = GeminiRequest.of(prompt, maxTokens);

        GeminiResponse response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/" + model + ":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(geminiRequest)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .block();

        return Optional.ofNullable(response)
                .map(GeminiResponse::getCandidates)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(GeminiResponse.Candidate::getContent)
                .map(GeminiResponse.Content::getParts)
                .filter(list -> !list.isEmpty())
                .map(parts -> parts.stream()
                        .map(GeminiResponse.Part::getText)
                        .filter(t -> t != null && !t.isBlank())
                        .collect(java.util.stream.Collectors.joining("\n")))
                .map(String::trim)
                .filter(s -> !s.isBlank());
    }

    // 재시도: Retry-After 존중 + 지수 백오프(+지터), 429/5xx만 재시도
    private Optional<String> callGeminiWithRetry(String prompt, int maxTokens, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return callGeminiAndExtractText(prompt, maxTokens);
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.warn("Gemini 호출 실패 ({}회 시도): {}", attempt, e.getMessage());
                    return Optional.empty();
                }

                long sleepMs;

                if (e instanceof WebClientResponseException) {
                    WebClientResponseException we = (WebClientResponseException) e;
                    int status = we.getStatusCode().value();

                    if (status == 429 || (status >= 500 && status < 600)) {
                        // Retry-After 우선 (초 또는 RFC1123)
                        String ra = we.getHeaders().getFirst("Retry-After");
                        Long raSeconds = parseRetryAfterSeconds(ra);

                        if (raSeconds != null) {
                            sleepMs = Math.max(0L, raSeconds * 1000L);
                        } else {
                            long base = Math.min(30, 1 << attempt) * 1000L; // 2,4,8,16,30s
                            long jitter = ThreadLocalRandom.current().nextLong(250, 1000);
                            sleepMs = base + jitter;
                        }
                        log.warn("Gemini 재시도 {}회차 - {}ms 후 (status={})", attempt, sleepMs, status);
                    } else {
                        log.warn("비재시도 에러(status={}) → 중단: {}", status, we.getMessage());
                        return Optional.empty();
                    }
                } else {
                    // 네트워크/타임아웃 등
                    long base = Math.min(30, 1 << attempt) * 1000L;
                    long jitter = ThreadLocalRandom.current().nextLong(250, 1000);
                    sleepMs = base + jitter;
                    log.warn("Gemini 재시도 {}회차 - {}ms 후 (네트워크/기타)", attempt, sleepMs);
                }

                try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
            }
        }
        return Optional.empty();
    }

    private static Long parseRetryAfterSeconds(String v) {
        if (v == null || v.isBlank()) return null;
        if (v.matches("\\d+")) return Long.parseLong(v); // seconds
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME);
            long millis = zdt.toInstant().toEpochMilli() - System.currentTimeMillis();
            return (millis > 0) ? (millis + 999) / 1000 : 0L;
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    // 뉴스 요약 및 성향 분석
    public Optional<SummaryAndInclination> summarizeAndIncline(String body) {
        String prompt =
                "다음 뉴스 본문을 한국어로 요약하고 성향을 판단해 JSON 한 줄로만 출력해주세요.\n" +
                        "- 요약: 핵심 사실/주체/조치/숫자/날짜·장소를 포함하고, 과장/추측/본문에 없는 정보는 금지\n" +
                        "- 요약은 반드시 400자 이내로 작성\n" +
                        "- 성향: '진보' | '중립' | '보수' 중 하나만 반환 (정확히 이 세 단어 중 하나)\n" +
                        "- 출력 형식: 반드시 아래 JSON 한 줄만 출력 (추가 텍스트/코드블록/주석 금지)\n\n" +
                        "{\"summary\": \"<요약문>\", \"inclination\": \"진보|중립|보수\"}\n\n" +
                        "뉴스 본문:\n" + body;

        return callGeminiWithRetry(prompt, maxTokensSummaryAndInclination, 3)
                .flatMap(this::parseSummaryAndInclination);
    }

    // JSON 파싱
    private Optional<SummaryAndInclination> parseSummaryAndInclination(String text) {
        if (text == null || text.isBlank()) return Optional.empty();

        try {
            JsonNode node = tolerantParse(text);
            if (node == null || !node.isObject()) {
                log.warn("Gemini JSON 파싱 실패: object 아님");
                return Optional.empty();
            }

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

    // 코드펜스/따옴표/앞뒤 잡문 보정 후 JSON 파싱
    private JsonNode tolerantParse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        try {
            // ```json ... ``` 코드펜스 제거
            if (s.startsWith("```")) {
                int end = s.indexOf("```", 3);
                if (end > 0) {
                    s = s.substring(3, end).replaceFirst("^json\\s*", "");
                }
            }
            // 스마트 따옴표 → 표준 따옴표
            s = s.replace('“', '"').replace('”', '"')
                    .replace('‘', '\'').replace('’', '\'');

            // JSON 덩어리만 추출(처음 '{' 또는 '['부터 마지막 '}' 또는 ']'까지)
            int obj = s.indexOf('{');
            int arr = s.indexOf('[');
            int si = -1;
            if (obj == -1) si = arr;
            else if (arr == -1) si = obj;
            else si = Math.min(obj, arr);
            if (si > 0) s = s.substring(si);
            int ei = Math.max(s.lastIndexOf('}'), s.lastIndexOf(']'));
            if (ei > 0 && ei + 1 < s.length()) s = s.substring(0, ei + 1);

            return objectMapper.readTree(s);
        } catch (Exception e) {
            log.warn("Gemini JSON 보정파싱 실패: {}", e.getMessage());
            return null;
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
