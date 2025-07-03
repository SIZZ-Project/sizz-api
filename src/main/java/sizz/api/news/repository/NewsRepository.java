package sizz.api.news.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sizz.api.news.entity.NewsDocument;

public interface NewsRepository extends MongoRepository<NewsDocument, String> {

    boolean existsByArticleId(String articleId);

}
