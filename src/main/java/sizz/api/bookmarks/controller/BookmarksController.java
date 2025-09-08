package sizz.api.bookmarks.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    @PatchMapping("/news/{articleId}/bookmark")
    public ResponseEntity<BookmarksResponse> toggleBookmark(
            @PathVariable String articleId,
            @RequestBody BookmarksRequest request
    ) {
        BookmarksResponse response = bookmarkService.toggleBookmark(request.getUserId(), articleId, request.isBookmarked());
        return ResponseEntity.ok(response);
    }

    //뉴스 전체 조회
    @GetMapping("/users/{userId}/bookmarks")
    public ResponseEntity<List<BookmarksResponse>> getBookmarks(
            @PathVariable String userId
    ) {
        List<BookmarksResponse> bookmarks = bookmarkService.getBookmarks(userId);
        return ResponseEntity.ok(bookmarks);
    }

    //북마크 여부 확인
    @GetMapping("/news/{articleId}/bookmark")
    public ResponseEntity<Boolean> isBookmarked(
            @PathVariable String articleId,
            @RequestParam String userId
    ) {
        boolean bookmarked = bookmarkService.isBookmarked(userId, articleId);
        return ResponseEntity.ok(bookmarked);
    }


}
