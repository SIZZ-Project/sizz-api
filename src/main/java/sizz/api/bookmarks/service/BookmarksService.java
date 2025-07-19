package sizz.api.bookmarks.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sizz.api.bookmarks.dto.BookmarksResponse;
import sizz.api.bookmarks.entity.BookmarksEntity;
import sizz.api.bookmarks.repository.BookmarksRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional

public class BookmarksService {
    private final BookmarksRepository bookmarksRepository;


    public BookmarksResponse toggleBookmark(Long userId, Long articleId, boolean bookmarked) {
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

    // 유저가 북마크한 목록 (bookmarked = true 인 것만 반환)
    public List<BookmarksResponse> getBookmarks(Long userId) {
        List<BookmarksEntity> entities = bookmarksRepository.findByUserId(userId);

        return entities.stream()
                .filter(BookmarksEntity::isBookmarked) // 활성화된 북마크만 필터링
                .map(BookmarksResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 특정 뉴스가 유저에 의해 북마크 되었는지 확인
    public boolean isBookmarked(Long userId, Long articleId) {
        return bookmarksRepository.findByUserIdAndArticleId(userId, articleId)
                .map(BookmarksEntity::isBookmarked)
                .orElse(false);
    }
}
