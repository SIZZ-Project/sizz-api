package sizz.api.bookmarks.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sizz.api.bookmarks.dto.BookmarksRequest;
import sizz.api.bookmarks.dto.BookmarksResponse;
import sizz.api.bookmarks.entity.BookmarksEntity;
import sizz.api.bookmarks.repository.BookmarksRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional

public class BookmarksService {
    private final BookmarksRepository bookmarksRepository;


    public BookmarksResponse addBookmark(BookmarksRequest request) {
        //중복 방지
        boolean exists = bookmarksRepository.existsByUserIdAndArticleId(request.getUserId(), request.getArticleId());
        if (exists) {
            throw new IllegalArgumentException("이미 북마크한 게시글입니다.");
        }

        BookmarksEntity bookmark = BookmarksEntity.builder()
                .userId(request.getUserId())
                .articleId(request.getArticleId())
                .build();

        BookmarksEntity saved = bookmarksRepository.save(bookmark);
        return BookmarksResponse.fromEntity(saved);
    }

    // 유저별 북마크 목록 조회
    @Transactional(readOnly = true)
    public List<BookmarksResponse> getBookmarksByUser(Long userId) {
        List<BookmarksEntity> bookmarks = bookmarksRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return bookmarks.stream()
                .map(BookmarksResponse::fromEntity)
                .collect(Collectors.toList());
    }


    public void deleteBookmark(Long bookmarkId) {
        if (!bookmarksRepository.existsById(bookmarkId)) {
            throw new IllegalArgumentException("북마크가 존재하지 않습니다.");
        }
        bookmarksRepository.deleteById(bookmarkId);
    }
}
