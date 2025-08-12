package sizz.api.like.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sizz.api.like.entity.LikeEntity;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {


    Optional<LikeEntity> findByUserIdAndArticleId(Long userId, Long articleId);


    List<LikeEntity> findByUserId(Long userId);
}
