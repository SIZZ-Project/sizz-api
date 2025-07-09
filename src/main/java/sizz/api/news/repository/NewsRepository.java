package sizz.api.news.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import sizz.api.news.entity.NewsDocument;

public interface NewsRepository extends MongoRepository<NewsDocument, String> {

    boolean existsByArticleId(String articleId);
    Page<NewsDocument> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);

}
