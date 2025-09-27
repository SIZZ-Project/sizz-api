package sizz.api.reaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {
    @NotBlank(message = "사용자 ID는 필수입니다.")
    private String userId;

    @NotNull(message = "반응은 필수입니다.")
    private ReactionType reaction;
}
