package sizz.api.reaction.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import sizz.api.community.post.repository.PostRepository;
import sizz.api.reaction.dto.ReactionResponse;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.entity.PostReactionEntity;
import sizz.api.reaction.repository.PostReactionRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostReactionService {

    private final PostReactionRepository postReactionRepository;
    private final PostRepository postRepository;

    @Transactional
    public ReactionResponse toggleReaction(String userId, Long postId, ReactionType newReaction) {
        ReactionType before = null; // delta 계산을 위해 이전 상태 보관

        try {
            Optional<PostReactionEntity> optional = postReactionRepository.findByUserIdAndPostId(userId, postId);

            PostReactionEntity doc;
            if (optional.isPresent()) {
                doc = optional.get();
                before = doc.getReaction();

                // 1) 동일 반응 → 삭제(토글 OFF)
                if (before == newReaction) {
                    postReactionRepository.delete(doc);

                    // after = null
                    int delta = -(before == ReactionType.LIKE ? 1 : 0);
                    if (delta != 0) postRepository.addLikeCount(postId, delta);

                    return ReactionResponse.fromPost(
                            PostReactionEntity.builder()
                                    .userId(userId)
                                    .postId(postId)
                                    .reaction(null)
                                    .build()
                    );
                }

                // 2) 다른 반응 → 변경
                doc.setReaction(newReaction);

            } else {
                // 3) 없으면 새로 생성
                doc = PostReactionEntity.builder()
                        .userId(userId)
                        .postId(postId)
                        .reaction(newReaction)
                        .build();
            }

            PostReactionEntity saved = postReactionRepository.save(doc);

            ReactionType after = saved.getReaction();
            int delta = (after == ReactionType.LIKE ? 1 : 0) - (before == ReactionType.LIKE ? 1 : 0);
            if (delta != 0) postRepository.addLikeCount(postId, delta);

            return ReactionResponse.fromPost(saved);

        } catch (DataIntegrityViolationException e) {
            // 유니크 충돌 재조회 → 최종 상태 기반으로 likeCount 보정
            var current = postReactionRepository.findByUserIdAndPostId(userId, postId).orElse(null);
            ReactionType after = (current == null) ? null : current.getReaction();

            int delta = ((after == ReactionType.LIKE) ? 1 : 0) - ((before == ReactionType.LIKE) ? 1 : 0);
            if (delta != 0) postRepository.addLikeCount(postId, delta);

            return (current == null)
                    ? ReactionResponse.fromPost(PostReactionEntity.builder()
                    .userId(userId).postId(postId).reaction(null).build())
                    : ReactionResponse.fromPost(current);
        }
    }

}
