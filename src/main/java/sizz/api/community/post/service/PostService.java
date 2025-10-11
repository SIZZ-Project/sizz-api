package sizz.api.community.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sizz.api.community.post.dto.PostRequest;
import sizz.api.community.post.dto.PostResponse;
import sizz.api.community.post.entity.PostEntity;
import sizz.api.community.post.repository.PostRepository;
import sizz.api.reaction.dto.ReactionType;
import sizz.api.reaction.entity.PostReactionEntity;
import sizz.api.reaction.repository.PostReactionRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;

    //게시글 작성
    @Transactional
    public PostResponse createPost(PostRequest request) {
        PostEntity post = PostEntity.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .likeCount(0)
                .commentCount(0)
                .build();

        PostEntity saved = postRepository.save(post);
        return PostResponse.fromEntity(saved, null);
    }

    //수정
    @Transactional
    public PostResponse updatePost(String userId, Long postId, PostRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인만 수정할 수 있습니다.");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImageUrl(request.getImageUrl());

        return PostResponse.fromEntity(post, null);
    }

    //삭제
    @Transactional
    public void deletePost(String userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }

    //커뮤니티 홈 - 최신글 10개
    public List<PostResponse> getRecentPosts(String userId) {
        var posts = postRepository.findTop10ByOrderByCreatedAtDesc();
        return mapWithMyReaction(userId, posts);
    }

    // 실시간 HOT - 오늘 날짜 + 좋아요 순 Top 5
    public List<PostResponse> getTodayHotPosts(String userId) {
        var startOfDay = LocalDate.now().atStartOfDay();
        var posts = postRepository.findTodayHotPosts(startOfDay)
                .stream().limit(5).toList();
        return mapWithMyReaction(userId, posts);
    }

    // 검색 (제목 + 내용)
    public List<PostResponse> searchPosts(String userId, String keyword) {
        var posts = postRepository.findByTitleContainingOrContentContaining(keyword, keyword);
        return mapWithMyReaction(userId, posts);
    }

    // 상세 조회
    public PostResponse getPost(String userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        ReactionType myReaction = null;
        if (userId != null) {
            myReaction = postReactionRepository.findByUserIdAndPostId(userId, postId)
                    .map(PostReactionEntity::getReaction)   // .name() 쓰지 않음
                    .orElse(null);
        }

        return PostResponse.fromEntity(post, myReaction);
    }

    private List<PostResponse> mapWithMyReaction(String userId, List<PostEntity> posts) {
        if (posts == null || posts.isEmpty()) return List.of();

        // 비로그인은 바로 null reaction으로 매핑
        if (userId == null) {
            return posts.stream()
                    .map(p -> PostResponse.fromEntity(p, null))
                    .toList();
        }

        // 배치 조회로 내 반응 맵 구성
        List<Long> ids = posts.stream().map(PostEntity::getId).toList();

        var reactionMap = postReactionRepository.findAllByUserIdAndPostIdIn(userId, ids)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        PostReactionEntity::getPostId,
                        PostReactionEntity::getReaction
                ));

        return posts.stream()
                .map(p -> PostResponse.fromEntity(p, reactionMap.get(p.getId())))
                .toList();
    }

}
