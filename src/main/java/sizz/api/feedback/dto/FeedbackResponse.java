package sizz.api.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
}
