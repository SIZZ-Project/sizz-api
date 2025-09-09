package sizz.api.like.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LikeRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private String userId;

    @NotNull(message = "게시글 ID는 필수입니다.")
    private String articleId;

    private  boolean liked;
}
