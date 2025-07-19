package sizz.api.comment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CommentRequest {
    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long articleId;
    @NotBlank(message = "작성자는 필수입니다.")
    private String userId;
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
}
