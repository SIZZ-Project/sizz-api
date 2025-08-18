package sizz.api.news.component;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import sizz.api.news.dto.NewsApiResponse;

@Component
@RequiredArgsConstructor
public class NewsDataIoAPI {

    private final WebClient newsDataIoWebClient;

    @Value("${newsdata.apiKey}")
    private String apiKey;

    public NewsApiResponse fetchNews(String keyword) {
        return newsDataIoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/latest")
                        .queryParam("apikey", apiKey)
                        .queryParam("q", keyword)
                        .queryParam("language", "ko")
                        .queryParam("full_content", 1)
                        .build())
                .retrieve()
                .bodyToMono(NewsApiResponse.class)
                .block();
    }

}
