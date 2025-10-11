package sizz.api.reaction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sizz.api.reaction.dto.ReactionRequest;
import sizz.api.reaction.dto.ReactionResponse;
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
        ReactionResponse response = reactionService.toggleReaction(email, articleId, request.getReaction());

        return ResponseEntity.ok(response);
    }

}
