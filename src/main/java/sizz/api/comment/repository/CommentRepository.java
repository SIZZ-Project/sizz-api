package sizz.api.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sizz.api.comment.entity.CommentEntity;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    //생성일 기준 오름차순 정렬하여 조회
    List<CommentEntity> findByArticleIdOrderByCreatedAtAsc(Long articleId);
}
