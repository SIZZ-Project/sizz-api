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

    @Scheduled(fixedRateString = "${newsdata.interval-ms}", initialDelay = 10_000)
    public void fetchNews(){

        try{
            NewsApiResponse response = newsDataIoAPI.fetchNews("정치");

            List<NewsDto> articles = (response != null) ? response.getResults() : null;

            if (articles == null || articles.isEmpty()) {
                log.info("뉴스 수집 결과 없음");
                return;
            }

            for(NewsDto article:articles){
                try {
                    String description = article.getDescription();
                    if (description != null && !description.isBlank()) {
                        //뉴스 요약
                        String summary = geminiAPI.summarizeNews(description);
                        article.setDescription(summary);

                        //뉴스 성향 분석
                        String inclination = geminiAPI.inclinationAnalysis(description);
                        article.setInclination(inclination);
                    }
                } catch (Exception ge) {
                    log.warn("뉴스 요약 오류 articleId={} msg={}", article.getArticleId(), ge.getMessage());
                }

                //Gemini API 분당 10회 제한 회피
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException ignored) {}
            }

            int saved = newsSyncService.syncNews(articles);
            log.info("[NEWS] fetch end - saved={}", saved);

        } catch (Exception e){
            log.error("뉴스 수집 오류: {}", e.getMessage(), e);
        }

    }

}
