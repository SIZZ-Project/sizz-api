package sizz.api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NewsDataIoConfig {

    @Value("${newsdata.url}")
    private String baseUrl;

    @Bean
    public WebClient newsDataIoWebClient(WebClient.Builder builder){
        return builder
                .baseUrl(baseUrl)
                .build();
    }

}
