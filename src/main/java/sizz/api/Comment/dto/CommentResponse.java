package sizz.api.Comment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String writer;
    private String content;
    private LocalDateTime createdAt;
}


