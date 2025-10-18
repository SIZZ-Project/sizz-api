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
import sizz.api.reaction.repository.NewsReactionRepository;
import sizz.api.bookmarks.repository.BookmarksRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsQueryService {

    private final NewsRepository newsRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOT_NEWS_CACHE_KEY = "hot:news";
    private final MongoTemplate mongo;
    private final NewsReactionRepository newsReactionRepository;
    private final BookmarksRepository bookmarksRepository;

    public Page<NewsResponseDto> findAllNewsPage(String userId, Pageable pageable) {
        // 1) 뉴스 페이지 조회
        Page<NewsDocument> page = newsRepository.findAll(pageable);

        // 2) 비로그인 → 유저 문맥 없이 반환
        if (userId == null || userId.isBlank()) {
            return page.map(NewsResponseDto::from);
        }

        // 3) 로그인 → 현재 페이지 기사들에 대한 나의 반응/북마크 배치 조회
        List<String> articleIds = page.getContent().stream()
                .map(NewsDocument::getArticleId)
                .toList();

        Map<String, ReactionType> myReactionMap = getMyReactionsMap(userId, articleIds);
        Map<String, Boolean>    myBookmarkMap = getMyBookmarksMap(userId, articleIds);

        // 4) DTO 변환 + 나의 반응/북마크 매핑
        return page.map(doc ->
                NewsResponseDto.fromWithEngagement(
                        doc,
                        myReactionMap.get(doc.getArticleId()),
                        Boolean.TRUE.equals(myBookmarkMap.get(doc.getArticleId()))
                )
        );
    }

    @SuppressWarnings("unchecked")
    public List<NewsResponseDto> getHotNews(String userId) {
        // 1) 캐시 조회 (뉴스만)
        List<NewsResponseDto> base =
                (List<NewsResponseDto>) redisTemplate.opsForValue().get(HOT_NEWS_CACHE_KEY);
        if (base == null) {
            base = refreshHotNewsCache();
        }

        // 2) 비로그인 → 캐시 그대로 반환
        if (userId == null || userId.isBlank()) {
            return base;
        }

        // 3) 로그인 → HOT 기사들에 대한 나의 반응/북마크 배치 조회
        List<String> ids = base.stream().map(NewsResponseDto::getArticleId).toList();
        Map<String, ReactionType> myReactionMap = getMyReactionsMap(userId, ids);
        Map<String, Boolean>      myBookmarkMap = getMyBookmarksMap(userId, ids); // ✅

        // 4) 캐시 오염 방지: 복사 DTO로 reaction/bookmarked 얹어서 반환
        return base.stream()
                .map(dto -> NewsResponseDto.builder()
                        .articleId(dto.getArticleId())
                        .title(dto.getTitle())
                        .description(dto.getDescription())
                        .link(dto.getLink())
                        .category(dto.getCategory())
                        .pubDate(dto.getPubDate())
                        .sourceName(dto.getSourceName())
                        .viewCount(dto.getViewCount())
                        .inclination(dto.getInclination())
                        .reactionType(myReactionMap.get(dto.getArticleId()))
                        .bookmarked(Boolean.TRUE.equals(myBookmarkMap.get(dto.getArticleId()))) // ✅
                        .build())
                .toList();
    }

    public List<NewsResponseDto> refreshHotNewsCache() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<NewsDocument> hotNews =
                newsRepository.findTop5ByPubDateBetweenOrderByViewCountDescPubDateDescIdDesc(
                        startOfDay, endOfDay);

        List<NewsResponseDto> result = hotNews.stream()
                .map(NewsResponseDto::from) // 뉴스만 캐시
                .toList();

        redisTemplate.opsForValue().set(HOT_NEWS_CACHE_KEY, result, Duration.ofHours(5));
        return result;
    }

    public CursorPage<NewsResponseDto> findAllNewsCursor(String userId, Integer limit, String after, String before) {
        // 뉴스만 커서로 조회
        CursorPage<NewsResponseDto> page =
                CursorUtils.run(mongo, new Criteria(), limit, after, before, NewsResponseDto::from);

        // 비로그인 → 그대로 반환
        if (userId == null || userId.isBlank()) {
            return page;
        }

        // 로그인 → 현재 커서 구간의 반응/북마크 배치 조회
        List<String> ids = page.getItems().stream()
                .map(NewsResponseDto::getArticleId)
                .toList();

        Map<String, ReactionType> myReactionMap = getMyReactionsMap(userId, ids);
        Map<String, Boolean>      myBookmarkMap = getMyBookmarksMap(userId, ids);

        // reaction/bookmarked 얹어서 새 리스트 구성
        List<NewsResponseDto> enriched = page.getItems().stream()
                .map(dto -> NewsResponseDto.builder()
                        .articleId(dto.getArticleId())
                        .title(dto.getTitle())
                        .description(dto.getDescription())
                        .link(dto.getLink())
                        .category(dto.getCategory())
                        .pubDate(dto.getPubDate())
                        .sourceName(dto.getSourceName())
                        .viewCount(dto.getViewCount())
                        .inclination(dto.getInclination())
                        .reactionType(myReactionMap.get(dto.getArticleId()))
                        .bookmarked(Boolean.TRUE.equals(myBookmarkMap.get(dto.getArticleId())))
                        .build())
                .toList();

        return new CursorPage<>(
                enriched,
                page.getNextCursor(),
                page.isHasNext(),
                page.getPrevCursor(),
                page.isHasPrev()
        );
    }

    public List<NewsResponseDto> fetchNewsByIds(List<String> articleIds) {
        List<NewsDocument> docs = newsRepository.findByArticleIdIn(articleIds);

        Map<String, NewsDocument> byId = docs.stream()
                .collect(Collectors.toMap(NewsDocument::getArticleId, d -> d));

        // 비로그인/공용 조회 → 뉴스만
        return articleIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(NewsResponseDto::from)
                .toList();
    }

    // 내 반응
    private Map<String, ReactionType> getMyReactionsMap(String userId, List<String> articleIds) {
        var myDocs = newsReactionRepository.findByUserIdAndArticleIdIn(userId, articleIds);
        return myDocs.stream()
                .collect(Collectors.toMap(
                        sizz.api.reaction.entity.NewsReactionDocument::getArticleId,
                        sizz.api.reaction.entity.NewsReactionDocument::getReaction,
                        (a, b) -> a
                ));
    }

    // 내 북마크
    private Map<String, Boolean> getMyBookmarksMap(String userId, List<String> articleIds) {
        var list = bookmarksRepository.findByUserIdAndArticleIdInAndBookmarkedTrue(userId, articleIds);
        return list.stream().collect(Collectors.toMap(
                sizz.api.bookmarks.entity.BookmarksEntity::getArticleId,
                e -> Boolean.TRUE,
                (a, b) -> a
        ));
    }
}
