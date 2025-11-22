package sizz.api.news.insight.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sizz.api.news.insight.document.UserArticleLogDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserArticleLogRepository extends MongoRepository<UserArticleLogDocument, String> {

    // userId + newsId 한 건
    Optional<UserArticleLogDocument> findByUserEmailAndNewsId(String userEmail, String newsId);

    // 특정 유저의 최근 본 로그
    List<UserArticleLogDocument> findByUserEmailAndLastViewedAtAfter(String userEmail, LocalDateTime lastViewedAt);
}