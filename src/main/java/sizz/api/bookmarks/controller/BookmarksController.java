package sizz.api.bookmarks.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sizz.api.bookmarks.dto.BookmarksRequest;
import sizz.api.bookmarks.dto.BookmarksResponse;
import sizz.api.bookmarks.service.BookmarksService;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarksController {
    private final BookmarksService bookmarkService;


    @PostMapping
    public ResponseEntity<BookmarksResponse> addBookmark(@RequestBody @Valid BookmarksRequest request) {
        BookmarksResponse response = bookmarkService.addBookmark(request);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<List<BookmarksResponse>> getBookmarksByUser(@RequestParam Long userId) {
        List<BookmarksResponse> bookmarks = bookmarkService.getBookmarksByUser(userId);
        return ResponseEntity.ok(bookmarks);
    }


    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable Long bookmarkId) {
        bookmarkService.deleteBookmark(bookmarkId);
        return ResponseEntity.noContent().build();
    }
}
