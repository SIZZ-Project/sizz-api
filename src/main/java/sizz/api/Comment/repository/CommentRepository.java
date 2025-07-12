package sizz.api.Comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sizz.api.Comment.entity.CommentEntity;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByArticleIdOrderByCreatedAtAsc(Long articleId);
}
