package sizz.api.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sizz.api.comment.dto.CommentRequest;
import sizz.api.comment.dto.CommentResponse;
import sizz.api.comment.service.CommentService;


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
        CommentResponse response = commentService.addComment(newsId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long newsId) {
        List<CommentResponse> comments = commentService.getComments(newsId);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
