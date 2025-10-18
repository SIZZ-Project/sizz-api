package sizz.api.reaction.service;

import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sizz.api.news.dto.NewsResponseDto;
import sizz.api.news.service.NewsQueryService;
import sizz.api.reaction.dto.ReactionResponse;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.entity.NewsReactionDocument;
import sizz.api.reaction.repository.NewsReactionRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NewsReactionService {

    private final NewsReactionRepository newsReactionRepository;
    private final NewsQueryService newsQueryService;

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

    public Slice<NewsResponseDto> getMyReactedNewsSlice(String userId, ReactionType type, Pageable pageable) {
        // 나의 뉴스 반응 슬라이스
        Slice<NewsReactionDocument> slice = newsReactionRepository.findByUserIdAndReaction(userId, type, pageable);

        List<String> ids = slice.getContent().stream()
                .map(NewsReactionDocument::getArticleId)
                .toList();

        if (ids.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, slice.hasNext());
        }

        // 뉴스 배치 조회 (입력 순서 유지)
        List<NewsResponseDto> newsDtos = newsQueryService.fetchNewsByIds(ids);

        // 입력 ids 순서대로 재정렬 + reactionType 채워서 반환
        Map<String, NewsResponseDto> byId =
                newsDtos.stream().collect(Collectors.toMap(NewsResponseDto::getArticleId, n -> n));

        List<NewsResponseDto> enriched = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(dto -> NewsResponseDto.builder()
                        .articleId(dto.getArticleId())
                        .title(dto.getTitle())
                        .description(dto.getDescription())
                        .link(dto.getLink())
                        .category(dto.getCategory())
                        .pubDate(dto.getPubDate())
                        .sourceName(dto.getSourceName())
                        .viewCount(dto.getViewCount())
                        .inclination(dto.getInclination())
                        .reactionType(type)
                        .build())
                .toList();

        return new SliceImpl<>(enriched, pageable, slice.hasNext());
    }
}