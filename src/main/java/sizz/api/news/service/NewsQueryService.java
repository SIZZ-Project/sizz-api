package sizz.api.news.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import sizz.api.core.pagination.CursorPage;
import sizz.api.news.dto.NewsResponseDto;
import sizz.api.news.entity.NewsDocument;
import sizz.api.news.repository.NewsRepository;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.entity.NewsReactionDocument;
import sizz.api.reaction.repository.NewsReactionRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static sizz.api.news.service.CursorUtils.run;

@Service
@RequiredArgsConstructor
public class NewsQueryService {

    private final NewsRepository newsRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOT_NEWS_CACHE_KEY = "hot:news";
    private final MongoTemplate mongo;
    private final NewsReactionRepository newsReactionRepository;

    public Page<NewsResponseDto> findAllNewsPage(String userId, Pageable pageable) {
        // 1) 뉴스 조회
        Page<NewsDocument> page = newsRepository.findAll(pageable);

        // 2) 비로그인인 경우 — 반응 조회 자체를 스킵하고 null 내려주기
        if (userId == null || userId.isBlank()) {
            return page.map(doc -> NewsResponseDto.from(doc, null));
        }

        // 3) 로그인인 경우 — 현재 페이지의 기사들에 대한 내 반응만 배치 조회
        List<String> articleIds = page.getContent().stream()
                .map(NewsDocument::getArticleId)
                .toList();

        var myDocs = newsReactionRepository.findByUserIdAndArticleIdIn(userId, articleIds);
        Map<String, ReactionType> myReactionMap = myDocs.stream()
                .collect(Collectors.toMap(
                        NewsReactionDocument::getArticleId,
                        NewsReactionDocument::getReaction,
                        (a, b) -> a
                ));

        // 4) DTO로 변환하면서 reactionType을 매핑, 없는 경우 null
        return page.map(doc ->
                NewsResponseDto.from(
                        doc,
                        myReactionMap.getOrDefault(doc.getArticleId(), null)
                )
        );
    }

    @SuppressWarnings("unchecked")
    public List<NewsResponseDto> getHotNews(String userId) {
        // 1) 캐시 조회 (없으면 갱신)
        List<NewsResponseDto> base = (List<NewsResponseDto>) redisTemplate.opsForValue().get(HOT_NEWS_CACHE_KEY);
        if (base == null) {
            base = refreshHotNewsCache();
        }

        // 2) 비로그인 or 사용자 반응 불필요 → 캐시 그대로 반환
        if (userId == null || userId.isBlank()) {
            return base;
        }

        // 3) 로그인: 현재 HOT 기사들에 대한 내 반응만 배치 조회
        List<String> ids = base.stream().map(NewsResponseDto::getArticleId).toList();
        var myDocs = newsReactionRepository.findByUserIdAndArticleIdIn(userId, ids);
        var myMap = myDocs.stream().collect(
                java.util.stream.Collectors.toMap(
                        NewsReactionDocument::getArticleId,
                        NewsReactionDocument::getReaction,
                        (a, b) -> a
                )
        );

        // 4) 캐시 객체 오염 방지: 복사본에 reactionType만 얹어서 반환
        return base.stream()
                .map(dto -> new NewsResponseDto(
                        dto.getArticleId(),
                        dto.getTitle(),
                        dto.getDescription(),
                        dto.getLink(),
                        dto.getCategory(),
                        dto.getPubDate(),
                        dto.getSourceName(),
                        dto.getViewCount(),
                        dto.getInclination(),
                        myMap.containsKey(dto.getArticleId()) ? myMap.get(dto.getArticleId()) : null
                ))
                .toList();
    }

    public List<NewsResponseDto> refreshHotNewsCache() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<NewsDocument> hotNews = newsRepository.findTop5ByPubDateBetweenOrderByViewCountDescPubDateDescIdDesc(startOfDay, endOfDay);
        List<NewsResponseDto> result = hotNews.stream()
                .map(doc -> NewsResponseDto.from(doc, null))
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(HOT_NEWS_CACHE_KEY, result, Duration.ofHours(5));
        return result;
    }

    public CursorPage<NewsResponseDto> findAllNewsCursor(String userId, Integer limit, String after, String before) {
        CursorPage<NewsResponseDto> page = run(mongo, new Criteria(), limit, after, before, doc -> NewsResponseDto.from(doc, null));

        // 비로그인 시 그대로 반환
        if (userId == null || userId.isBlank()) {
            return page;
        }

        // 로그인 사용자: 반응 붙이기
        List<String> ids = page.getItems().stream()    // ← getContent() 대신 getItems()
                .map(NewsResponseDto::getArticleId)
                .toList();

        var myDocs = newsReactionRepository.findByUserIdAndArticleIdIn(userId, ids);
        var myMap = myDocs.stream().collect(
                java.util.stream.Collectors.toMap(
                        sizz.api.reaction.entity.NewsReactionDocument::getArticleId,
                        sizz.api.reaction.entity.NewsReactionDocument::getReaction,
                        (a, b) -> a
                )
        );

        List<NewsResponseDto> enriched = page.getItems().stream()
                .map(dto -> new NewsResponseDto(
                        dto.getArticleId(),
                        dto.getTitle(),
                        dto.getDescription(),
                        dto.getLink(),
                        dto.getCategory(),
                        dto.getPubDate(),
                        dto.getSourceName(),
                        dto.getViewCount(),
                        dto.getInclination(),
                        myMap.containsKey(dto.getArticleId()) ? myMap.get(dto.getArticleId()) : null
                ))
                .toList();

        return new CursorPage<>(
                enriched,
                page.getNextCursor(),
                page.isHasNext(),
                page.getPrevCursor(),
                page.isHasPrev()
        );
    }

}