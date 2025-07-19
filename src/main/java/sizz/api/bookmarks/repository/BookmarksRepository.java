package sizz.api.bookmarks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sizz.api.bookmarks.entity.BookmarksEntity;
import java.util.List;
import java.util.Optional;

public interface BookmarksRepository extends JpaRepository<BookmarksEntity, Long> {

    // 북마크한 기록 조회
    Optional<BookmarksEntity> findByUserIdAndArticleId(Long userId, Long articleId);

    //북마크한 모든 기록 조회
    List<BookmarksEntity> findByUserId(Long userId);
}
