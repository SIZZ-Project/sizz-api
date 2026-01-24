package sizz.api.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import sizz.api.search.dto.InsightDto;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightCacheService {

    private final RedisTemplate<String, InsightDto> redisTemplate;

    private String key(String field) {
        return "insight:" + field;
    }

    public void save(InsightDto dto) {
        String redisKey = key(dto.getField());
        try {
            // TTL 3일
            redisTemplate.opsForValue().set(redisKey, dto, Duration.ofDays(3));
        } catch (Exception e) {
            log.error("[REDIS] Insight 저장 실패: {}", redisKey, e);
        }
    }

    public Optional<InsightDto> find(String field) {
        String redisKey = key(field);
        try {
            InsightDto dto = redisTemplate.opsForValue().get(redisKey);
            return Optional.ofNullable(dto);
        } catch (Exception e) {
            log.error("[REDIS] Insight 조회 실패: {}", redisKey, e);
            return Optional.empty();
        }
    }
}
