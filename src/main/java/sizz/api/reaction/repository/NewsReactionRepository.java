package sizz.api.reaction.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sizz.api.reaction.entity.NewsReactionDocument;

import java.util.List;
import java.util.Optional;

public interface NewsReactionRepository extends MongoRepository<NewsReactionDocument, String> {

    Optional<NewsReactionDocument> findByUserIdAndArticleId(String userId, String articleId);

    List<NewsReactionDocument> findByUserIdAndArticleIdIn(String userId, List<String> articleIds);

}
