package sizz.api.bookmarks.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import sizz.api.bookmarks.entity.BookmarksEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookmarksRepository extends JpaRepository<BookmarksEntity, Long> {

    // 특정 사용자가 특정 뉴스(articleId)를 북마크한 기록이 있는지 조회
    Optional<BookmarksEntity> findByUserIdAndArticleId(String userId, String articleId);

    // 특정 사용자가 북마크(true)한 모든 뉴스 목록을 슬라이스(Slice) 형태로 조회
    Slice<BookmarksEntity> findByUserIdAndBookmarkedTrue(String userId, Pageable pageable);

    // 여러 기사 ID(articleIds)에 대해 사용자가 북마크(true)한 기록만 한 번에 조회
    List<BookmarksEntity> findByUserIdAndArticleIdInAndBookmarkedTrue(String userId, Collection<String> articleIds);
}
