package sizz.api.news.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sizz.api.news.service.NewsQueryService;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotNewsScheduler {

    private final NewsQueryService newsQueryService;

    @Scheduled(cron = "0 0 */5 * * *")
    public void refreshHotNews() {
        log.info("실시간 HOT뉴스 캐시 갱신");
        newsQueryService.refreshHotNewsCache();
    }
}

