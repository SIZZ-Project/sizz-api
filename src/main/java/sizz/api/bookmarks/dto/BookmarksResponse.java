package sizz.api.bookmarks.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BookmarksResponse {
    private Long id;
    private Long userId;
    private Long articleId;
    private LocalDateTime createdAt;

    public static BookmarksResponse fromEntity(sizz.api.bookmarks.entity.BookmarksEntity bookmark) {
        return new BookmarksResponse(
                bookmark.getId(),
                bookmark.getUserId(),
                bookmark.getArticleId(),
                bookmark.getCreatedAt()
        );
    }
}
