package sizz.api.news.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sizz.api.news.entity.NewsDocument;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsViewScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MongoTemplate mongoTemplate;

    @Scheduled(fixedRateString = "${schedule.viewSyncMs}")
    public void persistViewCounts() {
        Set<String> keys = redisTemplate.keys("news:view:*");
        if (keys.isEmpty())
            return;

        for (String key : keys) {
            String articleId = key.replace("news:view:", "");
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) continue;

            long viewCount = Long.parseLong(value.toString());

            try {
                Query query = new Query(Criteria.where("articleId").is(articleId));
                Update update = new Update().inc("viewCount", viewCount);
                mongoTemplate.updateFirst(query, update, NewsDocument.class);

                redisTemplate.delete(key);
                log.info("조회수 반영 완료 - articleId: {} +{}", articleId, viewCount);
            } catch (Exception e) {
                log.error("조회수 반영 실패 - articleId: {} error: {}", articleId, e.getMessage(), e);
            }
        }
    }

}