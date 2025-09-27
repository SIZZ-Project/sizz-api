package sizz.api.reaction.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sizz.api.reaction.entity.NewsReactionDocument;

import java.util.Optional;

public interface NewsReactionRepository extends MongoRepository<NewsReactionDocument, String> {

    Optional<NewsReactionDocument> findByUserIdAndArticleId(String userId, String articleId);

}
