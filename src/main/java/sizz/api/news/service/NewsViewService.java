package sizz.api.news.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsViewService {

    private final StringRedisTemplate redisTemplate;

    public void increaseView(String articleId) {
        String redisKey = "news:view:" + articleId;
        redisTemplate.opsForValue().increment(redisKey);
    }

}
