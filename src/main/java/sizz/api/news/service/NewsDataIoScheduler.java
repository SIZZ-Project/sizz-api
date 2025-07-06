package sizz.api.news.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sizz.api.news.component.GeminiAPI;
import sizz.api.news.component.NewsDataIoAPI;
import sizz.api.news.dto.NewsApiResponse;
import sizz.api.news.dto.NewsDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsDataIoScheduler {

    private final NewsDataIoAPI newsDataIoAPI;
    private final NewsSyncService newsSyncService;
    private final GeminiAPI geminiAPI;

    @Scheduled(fixedRateString = "${newsdata.interval-ms}")
    public void fetchNews(){

        try{
            NewsApiResponse response = newsDataIoAPI.fetchNews("정치");

            List<NewsDto> articles = response.getResults();

            for(NewsDto article:articles){
                String description = article.getDescription();

                String summary = geminiAPI.summarizeNews(description);

                article.setDescription(summary);
            }

            newsSyncService.syncNews(articles);

        } catch (Exception e){
            log.error("뉴스 수집 오류: {}", e.getMessage(), e);
        }

    }

}
