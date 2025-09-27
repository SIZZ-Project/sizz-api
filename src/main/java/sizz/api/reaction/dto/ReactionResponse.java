package sizz.api.reaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sizz.api.reaction.entity.NewsReactionDocument;
import sizz.api.reaction.entity.PostReactionEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResponse {
    private String userId;
    private String targetId;
    private ReactionType reaction;

    // Document -> DTO 변환 메서드
    public static ReactionResponse fromNews(NewsReactionDocument document) {
        return new ReactionResponse(
                document.getUserId(),
                document.getArticleId(),
                document.getReaction()
        );
    }

    public static ReactionResponse fromPost(PostReactionEntity entity) {
        return new ReactionResponse(
                entity.getUserId(),
                String.valueOf(entity.getPostId()),
                entity.getReaction()
        );
    }
}