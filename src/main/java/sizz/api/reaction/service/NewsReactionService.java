package sizz.api.reaction.service;

import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sizz.api.reaction.dto.ReactionResponse;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.entity.NewsReactionDocument;
import sizz.api.reaction.repository.NewsReactionRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class NewsReactionService {

    private final NewsReactionRepository newsReactionRepository;

    public ReactionResponse toggleReaction(String userId, String articleId, ReactionType newReaction) {
        try {
            Optional<NewsReactionDocument> optional = newsReactionRepository.findByUserIdAndArticleId(userId, articleId);

            NewsReactionDocument doc;
            if (optional.isPresent()) {
                doc = optional.get();

                // 동일 반응이면 삭제(= 토글 off)
                if (doc.getReaction() == newReaction) {
                    newsReactionRepository.delete(doc);
                    return ReactionResponse.fromNews(
                            NewsReactionDocument.builder()
                                    .userId(userId)
                                    .articleId(articleId)
                                    .reaction(null)
                                    .build()
                    );
                }

                // 다른 반응이면 변경
                doc.setReaction(newReaction);

            } else {
                // 없으면 새로 생성
                doc = NewsReactionDocument.builder()
                        .userId(userId)
                        .articleId(articleId)
                        .reaction(newReaction)
                        .build();
            }

            NewsReactionDocument saved = newsReactionRepository.save(doc);
            return ReactionResponse.fromNews(saved);

        } catch (DuplicateKeyException e) {
            // 동시성으로 인해 유니크 제약 충돌 발생 시, 최신 상태 재조회해서 그대로 반환
            NewsReactionDocument current = newsReactionRepository
                    .findByUserIdAndArticleId(userId, articleId)
                    .orElse(null);

            if (current == null) {
                // 방금 삭제되었을 경우
                return ReactionResponse.fromNews(
                        NewsReactionDocument.builder()
                                .userId(userId)
                                .articleId(articleId)
                                .reaction(null)
                                .build()
                );
            }
            return ReactionResponse.fromNews(current);
        }
    }

    // 유저의 특정 뉴스에 대한 반응 조회
    public ReactionType getReaction(String userId, String articleId) {
        return newsReactionRepository.findByUserIdAndArticleId(userId, articleId)
                .map(NewsReactionDocument::getReaction)
                .orElse(null);
    }
}