package sizz.api.reaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {
    @NotNull(message = "반응은 필수입니다.")
    private ReactionType reaction;
}
