package sizz.api.reaction.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.entity.NewsReactionDocument;

import java.util.List;
import java.util.Optional;

public interface NewsReactionRepository extends MongoRepository<NewsReactionDocument, String> {

    Optional<NewsReactionDocument> findByUserIdAndArticleId(String userId, String articleId);

    List<NewsReactionDocument> findByUserIdAndArticleIdIn(String userId, List<String> articleIds);

    Slice<NewsReactionDocument> findByUserIdAndReaction(String userId, ReactionType reaction, Pageable pageable);

}
