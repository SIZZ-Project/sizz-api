package sizz.api.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sizz.api.comment.dto.CommentRequest;
import sizz.api.comment.dto.CommentResponse;
import sizz.api.comment.service.CommentService;


import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{articleId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String articleId,
            @RequestBody CommentRequest request
    ) {
        CommentResponse response = commentService.addComment(articleId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{articleId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable String articleId) {
        List<CommentResponse> comments = commentService.getComments(articleId);
        return ResponseEntity.ok(comments);
    }

    @PatchMapping("/{articleId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> patchComment(
            @PathVariable("articleId") String articleId,
            @PathVariable("commentId") Long commentId,
            @RequestBody CommentRequest request
    ) {
        CommentResponse updated = commentService.patchComment(articleId, commentId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{articleId}/comments/{commentsId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentsId) {
        commentService.deleteComment(commentsId);
        return ResponseEntity.noContent().build();
    }
}
