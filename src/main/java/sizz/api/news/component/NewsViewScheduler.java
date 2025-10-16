package sizz.api.news.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sizz.api.news.entity.NewsDocument;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsViewScheduler {

    private final StringRedisTemplate redisTemplate;
    private final MongoTemplate mongoTemplate;
    private final RedisConnectionFactory connectionFactory;

    /**
     * 일정 주기마다 Redis에 임시 저장된 조회수를 MongoDB에 반영하고,
     * Redis 값을 비워주는 작업
     */
    @Scheduled(fixedRateString = "${schedule.viewSyncMs}")
    public void persistViewCounts() {
        try (var conn = connectionFactory.getConnection();
            var cursor = conn.keyCommands().scan(
                    ScanOptions.scanOptions()
                            .match("news:view:*")   // news:view:로 시작하는 모든 키 검색
                            .count(1000)                   // 한 번에 최대 1000개씩 스캔
                            .build()
            )) {

            while (cursor.hasNext()) {
                // byte[] → String 변환
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                String articleId = key.substring("news:view:".length());

                // getAndDelete로 값 가져오면서 동시에 삭제
                String value = redisTemplate.opsForValue().getAndDelete(key);
                if (value == null) continue;

                long viewCount;
                try {
                    viewCount = Long.parseLong(value);
                } catch (NumberFormatException ex) {
                    log.warn("잘못된 조회수 값: key={} value={}", key, value);
                    continue;
                }

                // DB 업데이트
                try {
                    Query query = new Query(Criteria.where("articleId").is(articleId));
                    Update update = new Update().inc("viewCount", viewCount);
                    mongoTemplate.updateFirst(query, update, NewsDocument.class);

                    log.info("조회수 반영 완료 - articleId: {} +{}", articleId, viewCount);
                } catch (Exception e) {
                    log.error("조회수 반영 실패 - articleId: {} error: {}", articleId, e.getMessage(), e);
                    // 실패 시 Redis에 값 복구
                    redisTemplate.opsForValue().increment(key, viewCount);
                }
            }
        } catch (Exception e) {
            log.error("조회수 동기화 작업 전체 실패: {}", e.getMessage(), e);
        }
    }
}
