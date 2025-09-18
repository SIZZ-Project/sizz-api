package sizz.api.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sizz.api.news.component.GeminiAPI;
import sizz.api.search.dto.InsightDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightScheduler {

    private final GeminiAPI geminiAPI;
    private final InsightCacheService insightCacheService;

    @Scheduled(cron = "0 0 6 * * ?", zone = "Asia/Seoul")
    public void updateInsights() {
        String[] fields = {"경제", "정치", "사회", "문화", "과학", "세계"};

        for (String field : fields) {
            geminiAPI.generateKeywords(field).ifPresent(resultList -> {
                InsightDto dto = new InsightDto(field, resultList);
                insightCacheService.save(dto);

                log.info("[INSIGHT] {} 인사이트 저장 완료", field);
            });
        }
    }

}
