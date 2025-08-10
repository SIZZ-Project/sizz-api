package sizz.api.news.component;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import sizz.api.news.dto.GeminiRequest;
import sizz.api.news.dto.GeminiResponse;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GeminiAPI {

    private final WebClient geminiWebClient;

    @Value("${gemini.apiKey}")
    private String apiKey;

    @Value("${gemini.maxOutputTokens.summary}")
    private int maxTokensSummary;

    @Value("${gemini.maxOutputTokens.inclination}")
    private int maxTokensInclination;

    private Optional<String> callGeminiAndExtractText(String prompt, int maxTokens) {
        GeminiRequest geminiRequest = GeminiRequest.of(prompt, maxTokens);

        GeminiResponse response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-1.5-flash:generateContent")
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
        return callGeminiAndExtractText(prompt, maxTokensSummary).orElse("요약 실패");
    }

    public String inclinationAnalysis(String description) {
        String prompt = description + "\n이 뉴스 성향이 진보,중립,보수 중 어디에 해당하는지 알려주는데 답변을 '진보' 또는 '중립' 또는 '보수' 중 한 단어로만 정확히 답해줘.";

        String raw = callGeminiAndExtractText(prompt, maxTokensInclination).orElse("중립");

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
            default -> "중립";
        };
    }
}
