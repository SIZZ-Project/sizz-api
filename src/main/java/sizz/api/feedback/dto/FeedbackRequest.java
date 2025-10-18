package sizz.api.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(max = 1000, message = "최대 1000자까지 입력할 수 있어요.")
    private String content;
}
