package sizz.api.bookmarks.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sizz.api.bookmarks.dto.BookmarksRequest;
import sizz.api.bookmarks.dto.BookmarksResponse;
import sizz.api.bookmarks.service.BookmarksService;
import sizz.api.news.dto.NewsResponseDto;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class BookmarksController {
    private final BookmarksService bookmarkService;

    //토클
    @PatchMapping("/{articleId}/bookmark")
    public ResponseEntity<BookmarksResponse> toggleBookmark(
            @AuthenticationPrincipal String email,
            @PathVariable String articleId,
            @RequestBody BookmarksRequest request
    ) {
        if (email == null) return ResponseEntity.status(401).build();

        BookmarksResponse response = bookmarkService.toggleBookmark(email, articleId, request.isBookmarked());
        return ResponseEntity.ok(response);
    }

    //로그인한 사용자가 북마크한 뉴스 조회
    @GetMapping("/users/me/bookmarks")
    public ResponseEntity<Slice<NewsResponseDto>> getBookmarkNews(
            @AuthenticationPrincipal String email,
            Pageable pageable  // ?page=0&size=20&sort=createdAt,desc
    ) {
        if (email == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookmarkService.getBookmarkNewsSlice(email, pageable));
    }

    //북마크 여부 확인
    @GetMapping("/{articleId}/bookmark")
    public ResponseEntity<Boolean> isBookmarked(
            @AuthenticationPrincipal String email,
            @PathVariable String articleId
    ) {
        if (email == null) return ResponseEntity.status(401).build();

        boolean bookmarked = bookmarkService.isBookmarked(email, articleId);
        return ResponseEntity.ok(bookmarked);
    }
}
