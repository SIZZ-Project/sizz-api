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
    private final ArticleCrawlerService articleCrawlerService;

    @Scheduled(cron = "0 0 */3 * * ?", zone = "Asia/Seoul")
    public void fetchNews() {
        try {
            NewsApiResponse response = newsDataIoAPI.fetchNews("정치");
            List<NewsDto> articles = (response != null) ? response.getResults() : null;

            if (articles == null || articles.isEmpty()) {
                log.info("뉴스 수집 결과 없음");
                return;
            }

            for (NewsDto article : articles) {

                try {
                    // 기본 텍스트 : API가 준 description
                    String textForSummary = article.getDescription();

                    // 뉴스 본문 크롤링
                    try {
                        String url = article.getLink();

                        if (url != null && !url.isBlank()) {
                            String fullContent = articleCrawlerService.fetchArticle(url);

                            if (fullContent != null && !fullContent.isBlank()) {
                                textForSummary = fullContent;
                            }
                        }
                    } catch (Exception ce) {
                        log.warn("본문 크롤링 실패 articleId={} msg={}",
                                article.getArticleId(), ce.getMessage());
                    }

                    // 요약할 텍스트가 없으면 스킵
                    if (textForSummary == null || textForSummary.isBlank()) {
                        log.info("요약할 텍스트 없음 articleId={}", article.getArticleId());
                        continue;
                    }

                    // Gemini 요약 + 성향 분석
                    geminiAPI.summarizeAndIncline(textForSummary)
                            .ifPresent(r -> {
                                article.setDescription(r.summary());   // 요약으로 덮어쓰기
                                article.setInclination(r.inclination());
                            });

                } catch (Exception ge) {
                    log.warn("뉴스 요약 오류 articleId={} msg={}",
                            article.getArticleId(), ge.getMessage());
                }

                // 다음 기사 처리 전 1초 대기 (API / 크롤링 보호용)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }

            int saved = newsSyncService.syncNews(articles);
            log.info("[NEWS] fetch end - saved={}", saved);

        } catch (Exception e) {
            log.error("뉴스 수집 오류: {}", e.getMessage(), e);
        }
    }
}
