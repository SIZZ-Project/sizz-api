package sizz.api.like.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sizz.api.like.dto.LikeRequest;
import sizz.api.like.dto.LikeResponse;
import sizz.api.like.service.LikeService;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    //토클
    @PatchMapping("/news/{articleId}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            @PathVariable Long articleId,
            @RequestBody LikeRequest request
    ) {
        LikeResponse response = likeService.toggleLike(request.getUserId(), articleId, request.isLiked());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/users/{userId}/like")
    public ResponseEntity<List<LikeResponse>> getBookmarks(
            @PathVariable Long userId
    ) {
        List<LikeResponse> likes = likeService.getLike(userId);
        return ResponseEntity.ok(likes);
    }


    @GetMapping("/news/{articleId}/like")
    public ResponseEntity<Boolean> isLiked(
            @PathVariable Long articleId,
            @RequestParam Long userId
    ) {
        boolean liked = likeService.isliked(userId, articleId);
        return ResponseEntity.ok(liked);
    }
}
