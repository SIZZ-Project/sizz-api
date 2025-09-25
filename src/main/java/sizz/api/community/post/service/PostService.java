package sizz.api.community.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sizz.api.community.post.dto.PostRequest;
import sizz.api.community.post.dto.PostResponse;
import sizz.api.community.post.entity.PostEntity;
import sizz.api.community.post.repository.PostRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    //게시글 작성
    @Transactional
    public PostResponse createPost(PostRequest request) {
        PostEntity post = PostEntity.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .likeCount(0)
                .commentCount(0)
                .build();

        PostEntity saved = postRepository.save(post);
        return PostResponse.fromEntity(saved);
    }

    //수정
    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImageUrl(request.getImageUrl());
        
        return PostResponse.fromEntity(post);
    }
    
    //삭제
    @Transactional
    public void deletePost(Long id) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        postRepository.delete(post);
    }

    //커뮤니티 홈 - 최신글 10개
    public List<PostResponse> getRecentPosts() {
        return postRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(PostResponse::fromEntity)
                .toList();
    }


    //실시간 HOT - 오늘 날짜 + 좋아요 순 Top 5
    public List<PostResponse> getTodayHotPosts() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // 오늘 00:00
        return postRepository.findTodayHotPosts(startOfDay)
                .stream()
                .limit(5)
                .map(PostResponse::fromEntity)
                .toList();
    }

    //검색
    public List<PostResponse> searchPosts(String keyword) {
        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword)
                .stream()
                .map(PostResponse::fromEntity)
                .toList();
    }

    //상세 조회
    public PostResponse getPost(Long id) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        return PostResponse.fromEntity(post);
    }
}
