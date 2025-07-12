package sizz.api.news.component;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import sizz.api.news.dto.GeminiResponse;
import sizz.api.news.dto.GeminiRequest;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GeminiAPI {

    private final WebClient geminiWebClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.maxOutputTokens}")
    private int maxOutputTokens;

    public String summarizeNews(String description) {
        String prompt = description + "\n요약해줘";
        GeminiRequest geminiRequest = GeminiRequest.of(prompt, maxOutputTokens);

        GeminiResponse response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-1.5-flash:generateContent")
                        .queryParam("key", apiKey).build())
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
                .orElse("요약 실패");
    }

}
