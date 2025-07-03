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

    @Value("${newsdata.api-key}")
    private String apiKey;

    @Value("${newsdata.timeframe}")
    private String timeframe;

    public NewsApiResponse fetchNews(String keyword) {
        return newsDataIoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/latest")
                        .queryParam("apikey", apiKey)
                        .queryParam("q", keyword)
                        .queryParam("language", "ko")
                        .queryParam("timeframe", timeframe)
                        .build())
                .retrieve()
                .bodyToMono(NewsApiResponse.class)
                .block();
    }

}
