package sizz.api.bookmarks.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sizz.api.bookmarks.entity.BookmarksEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookmarksResponse {
    private Long id;
    private String userId;
    private String articleId;
    private Boolean bookmarked;

    // Entity -> DTO 변환 메서드
    public static BookmarksResponse fromEntity(BookmarksEntity entity) {
        return new BookmarksResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getArticleId(),
                entity.isBookmarked()
        );
    }
}