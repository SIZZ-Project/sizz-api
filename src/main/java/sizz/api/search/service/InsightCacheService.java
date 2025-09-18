package sizz.api.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import sizz.api.search.dto.InsightDto;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InsightCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private String key(String field){
        return "insight:" + field;
    }

    // 저장
    public void save(InsightDto dto) {
        redisTemplate.opsForValue().set(key(dto.getField()), dto);
    }

    // 조회
    public Optional<InsightDto> find(String field) {
        Object v = redisTemplate.opsForValue().get(key(field));
        return Optional.ofNullable((InsightDto) v);
    }


}
