package sizz.api.news.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ArticleCrawlerService {

    public String fetchArticle(String url) {
        // Playwright 리소스를 try-with-resources로 관리
        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            Page page = browser.newPage();
            page.navigate(url);

            // 네트워크/JS 로딩이 어느 정도 끝날 때까지 대기
            page.waitForLoadState(LoadState.NETWORKIDLE);

            String html = page.content();

            // Jsoup으로 HTML 파싱
            Document doc = Jsoup.parse(html, url);

            // 1) 우선 article 태그가 있으면 사용
            Element articleEl = doc.selectFirst("article");
            String text;

            if (articleEl != null) {
                text = articleEl.text();
            } else if (doc.body() != null) {
                // 2) 없으면 body 전체 텍스트 사용 (fallback)
                text = doc.body().text();
            } else {
                text = null;
            }

            if (text != null) {
                text = text.trim();
            }

            if (text == null || text.isBlank()) {
                log.warn("본문 추출 실패 url={}", url);
                return null;
            }

            return text;

        } catch (Exception e) {
            log.warn("크롤링 중 오류 url={} msg={}", url, e.getMessage(), e);
            return null;
        }
    }
}