package sizz.api.bookmarks.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookmarksRequest {
    @NotNull(message = "게시글 ID는 필수입니다.")
    private String articleId;

    private  boolean bookmarked;
}
