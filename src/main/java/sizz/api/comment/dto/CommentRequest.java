package sizz.api.comment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CommentRequest {
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
}
