package sizz.api.bookmarks.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sizz.api.bookmarks.dto.BookmarksResponse;
import sizz.api.bookmarks.entity.BookmarksEntity;
import sizz.api.bookmarks.repository.BookmarksRepository;
import sizz.api.news.dto.NewsResponseDto;
import sizz.api.news.service.NewsQueryService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarksService {
    private final BookmarksRepository bookmarksRepository;
    private final NewsQueryService newsQueryService;

    public BookmarksResponse toggleBookmark(String userId, String articleId, boolean bookmarked) {
        Optional<BookmarksEntity> optional = bookmarksRepository.findByUserIdAndArticleId(userId, articleId);

        BookmarksEntity entity;
        if (optional.isPresent()) {
            entity = optional.get();
            entity.setBookmarked(bookmarked);
        } else {
            entity = BookmarksEntity.builder()
                    .userId(userId)
                    .articleId(articleId)
                    .bookmarked(bookmarked)
                    .build();
        }

        BookmarksEntity saved = bookmarksRepository.save(entity);
        return BookmarksResponse.fromEntity(saved);
    }

    // 유저가 북마크한 뉴스 목록 (bookmarked = true 인 것만 반환)
    public Slice<NewsResponseDto> getBookmarkNewsSlice(String userId, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Slice<BookmarksEntity> slice = bookmarksRepository
                .findByUserIdAndBookmarkedTrue(userId, sorted);

        List<String> ids = slice.getContent().stream()
                .map(BookmarksEntity::getArticleId)
                .toList();

        if (ids.isEmpty()) {
            return new SliceImpl<>(List.of(), sorted, slice.hasNext());
        }

        // 뉴스 데이터만 조회
        List<NewsResponseDto> newsDtos = newsQueryService.fetchNewsByIds(ids);

        // 입력 순서 정렬 + bookmarked true 세팅
        Map<String, NewsResponseDto> dtoById = newsDtos.stream()
                .collect(Collectors.toMap(NewsResponseDto::getArticleId, d -> d));

        List<NewsResponseDto> enriched = ids.stream()
                .map(dtoById::get)
                .filter(Objects::nonNull)
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
                        .bookmarked(true)
                        .build())
                .toList();

        return new SliceImpl<>(enriched, sorted, slice.hasNext());
    }


    // 특정 뉴스가 유저에 의해 북마크 되었는지 확인
    public boolean isBookmarked(String userId, String articleId) {
        return bookmarksRepository.findByUserIdAndArticleId(userId, articleId)
                .map(BookmarksEntity::isBookmarked)
                .orElse(false);
    }
}
