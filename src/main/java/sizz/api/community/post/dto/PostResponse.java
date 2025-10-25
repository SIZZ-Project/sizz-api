package sizz.api.community.post.dto;

import lombok.Builder;
import lombok.Getter;
import sizz.api.community.post.entity.PostEntity;
import sizz.api.reaction.dto.ReactionType;

@Getter
@Builder
public class PostResponse {

    private Long id;
    private String userId;
    private String title;
    private String content;
    private String imageUrl;
    private int likeCount;
    private int commentCount;
    private String createdAt;
    private ReactionType myReaction;

    // Entity → DTO
    public static PostResponse fromEntity(PostEntity post, ReactionType myReaction) {
        return fromEntity(post, myReaction, null);
    }

    public static PostResponse fromEntity(PostEntity post, ReactionType myReaction, String overrideImageUrl) {
        String image = (overrideImageUrl != null) ? overrideImageUrl : post.getImageUrl();

        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrl(image)
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .myReaction(myReaction)
                .build();
    }

}
