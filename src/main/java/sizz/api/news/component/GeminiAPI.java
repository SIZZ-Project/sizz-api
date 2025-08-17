package sizz.api.news.component;

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

    @Value("${gemini.maxOutputTokens.summary}")
    private int maxTokensSummary;

    @Value("${gemini.maxOutputTokens.inclination}")
    private int maxTokensInclination;

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
                .map(list -> list.get(0))
                .map(GeminiResponse.Part::getText)
                .map(String::trim)
                .filter(s -> !s.isBlank());
    }

    public String summarizeNews(String description) {
        String prompt = description + "\n요약해줘";
        return callGeminiWithRetry(prompt, maxTokensSummary, 3).orElse("요약 실패");
    }

    public String inclinationAnalysis(String description) {
        String prompt = description + "\n이 뉴스 성향이 진보,중립,보수 중 어디에 해당하는지 알려주는데 답변을 '진보' 또는 '중립' 또는 '보수' 중 한 단어로만 정확히 답해줘.";

        String raw = callGeminiWithRetry(prompt, maxTokensInclination, 3).orElse("분석 실패");

        String normalized = raw.toLowerCase(Locale.ROOT)
                .replace("progressive", "진보")
                .replace("conservative", "보수")
                .replace("neutral", "중립")
                .replaceAll("[^가-힣]", "")
                .replaceAll(".*(진보|중립|보수).*", "$1")
                .trim();


        return switch (normalized) {
            case "진보" -> "진보";
            case "보수" -> "보수";
            case "중립" -> "중립";
            default -> "분석 실패";
        };
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
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
        }
        return Optional.empty();
    }

}
