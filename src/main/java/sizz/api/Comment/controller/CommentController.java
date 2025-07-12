package sizz.api.Comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sizz.api.Comment.dto.CommentRequest;
import sizz.api.Comment.dto.CommentResponse;
import sizz.api.Comment.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/news/{newsId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long newsId,
            @RequestBody CommentRequest request
    ) {
        return ResponseEntity.ok(commentService.addComment(newsId, request));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long newsId) {
        return ResponseEntity.ok(commentService.getComments(newsId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
