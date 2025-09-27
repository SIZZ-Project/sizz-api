package sizz.api.reaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sizz.api.reaction.entity.PostReactionEntity;

import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReactionEntity, Long> {

    Optional<PostReactionEntity> findByUserIdAndPostId(String userId, Long postId);

}
