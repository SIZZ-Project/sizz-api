package sizz.api.news.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
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