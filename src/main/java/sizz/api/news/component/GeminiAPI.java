package sizz.api.news.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sizz.api.news.dto.GeminiRequest;
import sizz.api.news.dto.GeminiResponse;

import java.util.Locale;
import java.util.Optional;

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

        try {
            GeminiResponse response = geminiWebClient.post()
                    .uri(b -> b.path("/v1beta/models/" + model + ":generateContent")
                            .queryParam("key", apiKey).build())
                    .bodyValue(geminiRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class).flatMap(body -> {
                                log.error("[Gemini] HTTP {}: {}", r.statusCode(), body);
                                return Mono.error(new IllegalStateException("Gemini API error " + r.statusCode()));
                            })
                    )
                    .bodyToMono(GeminiResponse.class)
                    .block();

            log.info("[Gemini] Raw response: {}", (response == null ? "null" : objectMapper.writeValueAsString(response)));

            return Optional.ofNullable(response)
                    .map(GeminiResponse::getCandidates)
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0))
                    .map(GeminiResponse.Candidate::getContent)
                    .map(GeminiResponse.Content::getParts)
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0))
                    .map(GeminiResponse.Part::getText)
                    .map(String::trim)
                    .map(GeminiAPI::extractJson) // ← JSON만 뽑기
                    .filter(s -> !s.isBlank());

        } catch (Exception e) {
            log.error("[Gemini] 호출/파싱 실패: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /** 모델이 ```json ... ``` 같은 포맷을 줄 때 대비해 JSON 객체만 추출 */
    private static String extractJson(String raw){
        if (raw == null) return null;
        String s = raw.replace("```json","").replace("```","").trim();
        int st = s.indexOf('{'), ed = s.lastIndexOf('}');
        return (st>=0 && ed>st) ? s.substring(st, ed+1).trim() : s;
    }


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
                long delay = 500L * attempt;
                log.info("Gemini 재시도 {}회차 - {}ms 후 재시도", attempt, delay);
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
            }
        }
        return Optional.empty();
    }

    // 뉴스 요약 및 성향 분석
    public Optional<SummaryAndInclination> summarizeAndIncline(String description) {
        String prompt =
                "다음 뉴스 본문을 한국어로 400자 이내로 요약하고, 성향을 판단해 JSON 한 줄만 출력하세요.\n" +
                        "- 요약: 핵심 사실/주체/조치/숫자/날짜·장소 포함\n" +
                        "- 길이: 400자 이내 (한 문단으로 간결하게)\n" +
                        "- 성향: '진보'|'중립'|'보수' 중 하나 (모호하면 '중립')\n" +
                        "- 출력 형식: {\"summary\":\"...\",\"inclination\":\"진보|중립|보수\"}\n\n" +
                        "뉴스 본문:\n" + description;

        return callGeminiWithRetry(prompt, maxTokensSummaryAndInclination, 3)
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