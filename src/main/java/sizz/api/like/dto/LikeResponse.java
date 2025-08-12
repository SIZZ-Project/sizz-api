package sizz.api.like.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sizz.api.like.entity.LikeEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {
    private Long id;
    private Long userId;
    private Long articleId;
    private Boolean liked;

    // Entity -> DTO 변환 메서드
    public static sizz.api.like.dto.LikeResponse fromEntity(LikeEntity entity) {
        return new sizz.api.like.dto.LikeResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getArticleId(),
                entity.isLiked()
        );
    }
}