package sizz.api.reaction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sizz.api.news.dto.NewsResponseDto;
import sizz.api.reaction.dto.ReactionRequest;
import sizz.api.reaction.dto.ReactionResponse;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.service.NewsReactionService;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsReactionController {

    private final NewsReactionService reactionService;

    //토클
    @PatchMapping("/{articleId}/reaction")
    public ResponseEntity<ReactionResponse> toggleReaction(
            @AuthenticationPrincipal String email,
            @PathVariable String articleId,
            @Valid @RequestBody ReactionRequest request
    ) {
        if (email == null) return ResponseEntity.status(401).build();
        ReactionResponse response = reactionService.toggleReaction(email, articleId, request.getReaction());

        return ResponseEntity.ok(response);
    }

    // 사용자가 반응한 뉴스 목록
    @GetMapping("/users/me/reactions")
    public ResponseEntity<Slice<NewsResponseDto>> getMyReactedNews(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "LIKE") ReactionType type,
            Pageable pageable
    ) {
        if (email == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reactionService.getMyReactedNewsSlice(email, type, pageable));
    }

}
