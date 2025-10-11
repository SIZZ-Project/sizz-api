package sizz.api.bookmarks.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sizz.api.bookmarks.dto.BookmarksRequest;
import sizz.api.bookmarks.dto.BookmarksResponse;
import sizz.api.bookmarks.service.BookmarksService;

import java.util.List;

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

    //뉴스 전체 조회
    @GetMapping("/users/me/bookmarks")
    public ResponseEntity<List<BookmarksResponse>> getBookmarks(
            @AuthenticationPrincipal String email
    ) {
        if (email == null) return ResponseEntity.status(401).build();

        List<BookmarksResponse> bookmarks = bookmarkService.getBookmarks(email);
        return ResponseEntity.ok(bookmarks);
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
