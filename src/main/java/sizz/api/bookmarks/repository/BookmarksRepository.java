package sizz.api.bookmarks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sizz.api.bookmarks.entity.BookmarksEntity;
import java.util.List;

public interface BookmarksRepository extends JpaRepository<BookmarksEntity, Long> {

    // 특정 유저가 북마크한 목록을 생성일자 내림차순으로 조회
    List<BookmarksEntity> findByUserIdOrderByCreatedAtDesc(Long userId);



    // 특정 유저가 특정 게시글을 북마크했는지 확인
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);
}
