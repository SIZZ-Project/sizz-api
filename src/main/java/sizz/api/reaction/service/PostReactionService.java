package sizz.api.reaction.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import sizz.api.reaction.dto.ReactionResponse;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.entity.PostReactionEntity;
import sizz.api.reaction.repository.PostReactionRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostReactionService {

    private final PostReactionRepository postReactionRepository;

    @Transactional
    public ReactionResponse toggleReaction(String userId, Long postId, ReactionType newReaction) {
        try {
            Optional<PostReactionEntity> optional = postReactionRepository.findByUserIdAndPostId(userId, postId);

            PostReactionEntity doc;
            if (optional.isPresent()) {
                doc = optional.get();

                // 동일 반응이면 삭제(= 토글 off)
                if (doc.getReaction() == newReaction) {
                    postReactionRepository.delete(doc);
                    return ReactionResponse.fromPost(
                            PostReactionEntity.builder()
                                    .userId(userId)
                                    .postId(postId)
                                    .reaction(null)
                                    .build()
                    );
                }

                // 다른 반응이면 변경
                doc.setReaction(newReaction);

            } else {
                // 없으면 새로 생성
                doc = PostReactionEntity.builder()
                        .userId(userId)
                        .postId(postId)
                        .reaction(newReaction)
                        .build();
            }

            PostReactionEntity saved = postReactionRepository.save(doc);
            return ReactionResponse.fromPost(saved);

        } catch (DataIntegrityViolationException e) {
            // 유니크 충돌 재조회
            var current = postReactionRepository.findByUserIdAndPostId(userId, postId).orElse(null);
            return (current == null)
                    ? ReactionResponse.fromPost(PostReactionEntity.builder()
                    .userId(userId).postId(postId).reaction(null).build())
                    : ReactionResponse.fromPost(current);
        }
    }

    // 유저의 특정 게시글 대한 반응 조회
    @Transactional(readOnly = true)
    public ReactionType getReaction(String userId, Long postId) {
        return postReactionRepository.findByUserIdAndPostId(userId, postId)
                .map(PostReactionEntity::getReaction)
                .orElse(null);
    }
    
}
