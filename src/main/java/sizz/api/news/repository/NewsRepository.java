package sizz.api.news.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sizz.api.news.entity.NewsDocument;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NewsRepository extends MongoRepository<NewsDocument, String> {

    boolean existsByArticleId(String articleId);
    List<NewsDocument> findTop5ByCreatedAtBetweenOrderByViewCountDescCreatedAtDescIdDesc(LocalDateTime start, LocalDateTime end);
    List<NewsDocument> findByArticleIdIn(Collection<String> articleIds);
    List<NewsDocument> findByIdIn(List<String> ids);

}
