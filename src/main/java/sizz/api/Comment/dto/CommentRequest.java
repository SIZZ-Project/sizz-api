package sizz.api.Comment.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private String writer;
    private String content;
}
