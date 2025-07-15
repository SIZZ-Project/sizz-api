package sizz.api.bookmarks.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookmarksRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long articleId;
}
