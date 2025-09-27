package sizz.api.reaction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sizz.api.reaction.dto.ReactionRequest;
import sizz.api.reaction.dto.ReactionResponse;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.service.PostReactionService;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostReactionController {

    private final PostReactionService postReactionService;

    //토클
    @PatchMapping("/{postId}/reaction")
    public ResponseEntity<ReactionResponse> toggleReaction(
            @PathVariable Long postId,
            @Valid @RequestBody ReactionRequest request
    ) {
        ReactionResponse response = postReactionService.toggleReaction(request.getUserId(), postId, request.getReaction());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}/reaction")
    public ResponseEntity<ReactionType> getReaction(
            @PathVariable Long postId,
            @RequestParam String userId
    ) {
        ReactionType reaction = postReactionService.getReaction(userId, postId);

        return (reaction == null) ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(reaction);
    }
}
