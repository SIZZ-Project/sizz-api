package sizz.api.like.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sizz.api.like.dto.LikeResponse;
import sizz.api.like.entity.LikeEntity;
import sizz.api.like.repository.LikeRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional

public class LikeService {
    private final LikeRepository likeRepository;


    public LikeResponse toggleLike(String userId, String articleId, boolean liked) {
        Optional<LikeEntity> optional = likeRepository.findByUserIdAndArticleId(userId, articleId);

        LikeEntity entity;
        if (optional.isPresent()) {
            entity = optional.get();
            entity.setLiked(liked);
        } else {
            entity = LikeEntity.builder()
                    .userId(userId)
                    .articleId(articleId)
                    .liked(liked)
                    .build();
        }

        LikeEntity saved = likeRepository.save(entity);
        return LikeResponse.fromEntity(saved);
    }

    public List<LikeResponse> getLike(String userId) {
        List<LikeEntity> entities = likeRepository.findByUserId(userId);

        return entities.stream()
                .filter(LikeEntity::isLiked) // 활성화된 북마크만 필터링
                .map(LikeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 특정 뉴스가 유저에 의해 북마크 되었는지 확인
    public boolean isliked(String userId, String articleId) {
        return likeRepository.findByUserIdAndArticleId(userId, articleId)
                .map(LikeEntity::isLiked)
                .orElse(false);
    }
}