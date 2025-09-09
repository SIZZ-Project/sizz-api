package sizz.api.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String articleId;
    private String userId;
    private String content;
    private LocalDateTime createdAt;

    public static CommentResponse fromEntity(sizz.api.comment.entity.CommentEntity comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getArticleId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}


