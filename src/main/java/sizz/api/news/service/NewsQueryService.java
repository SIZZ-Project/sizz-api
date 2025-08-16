package sizz.api.news.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import sizz.api.news.dto.NewsResponseDto;
import sizz.api.news.entity.NewsDocument;
import sizz.api.news.repository.NewsRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsQueryService {

    private final NewsRepository newsRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOT_NEWS_CACHE_KEY = "hot:news";

    public Page<NewsResponseDto> findAllNews(Pageable pageable) {
        return newsRepository.findAll(pageable)
                .map(NewsResponseDto::from);
    }

    public List<NewsResponseDto> getHotNews() {
        List<NewsResponseDto> cached = (List<NewsResponseDto>) redisTemplate.opsForValue().get(HOT_NEWS_CACHE_KEY);
        if (cached != null) return cached;

        return refreshHotNewsCache();
    }

    public List<NewsResponseDto> refreshHotNewsCache() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<NewsDocument> hotNews = newsRepository.findTop5ByPubDateBetweenOrderByViewCountDescPubDateDescIdDesc(startOfDay, endOfDay);
        List<NewsResponseDto> result = hotNews.stream()
                .map(NewsResponseDto::from)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(HOT_NEWS_CACHE_KEY, result, Duration.ofHours(5));
        return result;
    }

}
