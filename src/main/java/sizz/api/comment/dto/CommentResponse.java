package sizz.api.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long articleId;
    private String writer;
    private String content;
    private LocalDateTime createdAt;

    public static CommentResponse fromEntity(sizz.api.comment.entity.CommentEntity comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getArticleId(),
                comment.getWriter(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}


