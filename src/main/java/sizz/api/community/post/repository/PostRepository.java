package sizz.api.community.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sizz.api.community.post.entity.PostEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    // 커뮤니티 홈 (최신글 10개)
    List<PostEntity> findTop10ByOrderByCreatedAtDesc();

    // 실시간 HOT (오늘 날짜 + 좋아요 순 5개)
    @Query("SELECT p FROM PostEntity p " +
            "WHERE p.createdAt >= :startOfDay " +
            "ORDER BY p.likeCount DESC")
    List<PostEntity> findTodayHotPosts(@Param("startOfDay") LocalDateTime startOfDay);

    //검색
    List<PostEntity> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword);

    //좋아요 수 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PostEntity p set p.likeCount = p.likeCount + :delta where p.id = :postId")
    void addLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

}
