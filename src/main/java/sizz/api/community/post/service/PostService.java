package sizz.api.community.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sizz.api.community.post.dto.PostRequest;
import sizz.api.community.post.dto.PostResponse;
import sizz.api.community.post.entity.PostEntity;
import sizz.api.community.post.repository.PostRepository;
import sizz.api.global.s3.StorageService;
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
    private final StorageService storageService;

    //게시글 작성
    @Transactional
    public PostResponse createPost(String email, PostRequest request, MultipartFile image) {
        String key = null;
        if (image != null && !image.isEmpty()) {
            key = storageService.uploadImage(image, "post");
        }

        PostEntity post = PostEntity.builder()
                .userId(email)
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(key)
                .likeCount(0)
                .commentCount(0)
                .build();

        PostEntity saved = postRepository.save(post);

        return toPostResponseWithPresignedUrl(saved, null);
    }

    //수정
    @Transactional
    public PostResponse updatePost(String userId, Long postId, PostRequest request, MultipartFile image) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인만 수정할 수 있습니다.");
        }

        // 새 이미지 업로드 시 기존 이미지 삭제 후 교체
        if (image != null && !image.isEmpty()) {
            String oldKey = post.getImageUrl();
            String newKey = storageService.uploadImage(image, "post");
            post.setImageUrl(newKey);

            if (oldKey != null) {
                storageService.delete(oldKey);
            }
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        return toPostResponseWithPresignedUrl(post, null);
    }


    //삭제
    @Transactional
    public void deletePost(String userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인만 삭제할 수 있습니다.");
        }

        // 이미지가 있으면 S3에서도 삭제
        if (post.getImageUrl() != null) {
            storageService.delete(post.getImageUrl());
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
                    .map(PostReactionEntity::getReaction)
                    .orElse(null);
        }

        return toPostResponseWithPresignedUrl(post, myReaction);
    }

    private List<PostResponse> mapWithMyReaction(String userId, List<PostEntity> posts) {
        if (posts == null || posts.isEmpty()) return List.of();

        // presigned URL을 붙여주는 헬퍼
        java.util.function.Function<PostEntity, String> urlOf = p ->
                (p.getImageUrl() == null) ? null
                        : storageService.presignedGetUrl(p.getImageUrl(), 30); // 30분

        // 비로그인은 바로 매핑
        if (userId == null) {
            return posts.stream()
                    .map(p -> PostResponse.fromEntity(p, null, urlOf.apply(p)))
                    .toList();
        }

        // 내 반응 맵 구성
        List<Long> ids = posts.stream().map(PostEntity::getId).toList();
        var reactionMap = postReactionRepository.findAllByUserIdAndPostIdIn(userId, ids)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        PostReactionEntity::getPostId,
                        PostReactionEntity::getReaction
                ));

        return posts.stream()
                .map(p -> PostResponse.fromEntity(p, reactionMap.get(p.getId()), urlOf.apply(p)))
                .toList();
    }

    // presigned URL을 붙여서 응답으로 변환하는 공통 함수
    private PostResponse toPostResponseWithPresignedUrl(PostEntity post, ReactionType reaction) {
        String url = (post.getImageUrl() == null)
                ? null
                : storageService.presignedGetUrl(post.getImageUrl(), 30);
        return PostResponse.fromEntity(post, reaction, url);
    }

}
