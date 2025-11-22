package sizz.api.news.insight.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sizz.api.news.insight.document.UserArticleLogDocument;
import sizz.api.news.insight.repository.UserArticleLogRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsViewLogService {

    private final UserArticleLogRepository userArticleLogRepository;

    /**
     * 유저가 뉴스를 볼 때 호출됨.
     * - 처음 보는 뉴스면 새 로그 생성
     * - 이미 본 뉴스면 조회수(viewCount) 증가 + 마지막 본 시간(lastViewedAt)만 갱신
     */
    public void logNewsView(String email, String newsId) {
        if (email == null) {
            return; // 비로그인 유저는 기록하지 않음
        }

        LocalDateTime now = LocalDateTime.now();

        Optional<UserArticleLogDocument> optionalLog =
                userArticleLogRepository.findByUserEmailAndNewsId(email, newsId);

        if (optionalLog.isPresent()) {
            // 이미 본 뉴스 → 조회수 증가 + 마지막 본 시간 갱신
            UserArticleLogDocument log = optionalLog.get();
            long currentCount = (log.getViewCount() == 0 ? 0 : log.getViewCount());
            log.setViewCount(currentCount + 1);
            log.setLastViewedAt(now);
            userArticleLogRepository.save(log);

        } else {
            // 처음 보는 뉴스 → 새 로그 생성
            UserArticleLogDocument log = UserArticleLogDocument.builder()
                    .userEmail(email)
                    .newsId(newsId)
                    .firstViewedAt(now)
                    .lastViewedAt(now)
                    .viewCount(1L)
                    .dwellTimeSec(0)
                    .build();

            userArticleLogRepository.save(log);
        }
    }

    /**
     * 페이지를 나갈 때 머문시간을 보내면 누적함.
     * - dwellTimeSec = 총 머문 시간(초)
     */
    public void addDwellTime(String email, String newsId, int dwellTimeToAddSec) {
        if (email == null) {
            return;
        }

        userArticleLogRepository.findByUserEmailAndNewsId(email, newsId)
                .ifPresent(log -> {
                    int current = (log.getDwellTimeSec() == null) ? 0 : log.getDwellTimeSec();
                    log.setDwellTimeSec(current + dwellTimeToAddSec);
                    userArticleLogRepository.save(log);
                });
    }
}