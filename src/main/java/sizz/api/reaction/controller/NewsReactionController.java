package sizz.api.reaction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            @PathVariable String articleId,
            @Valid @RequestBody ReactionRequest request
    ) {
        ReactionResponse response = reactionService.toggleReaction(request.getUserId(), articleId, request.getReaction());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{articleId}/reaction")
    public ResponseEntity<ReactionType> getReaction(
            @PathVariable String articleId,
            @RequestParam String userId
    ) {
        ReactionType reaction = reactionService.getReaction(userId, articleId);

        return (reaction == null) ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(reaction);
    }
}
