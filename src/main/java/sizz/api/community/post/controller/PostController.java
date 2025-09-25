package sizz.api.community.post.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sizz.api.community.post.dto.PostRequest;
import sizz.api.community.post.dto.PostResponse;
import sizz.api.community.post.service.PostService;
import sizz.api.community.common.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    //작성
    @PostMapping("{userId}/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@RequestBody PostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //수정
    @PutMapping("{userId}/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @RequestBody PostRequest request) {
        PostResponse response = postService.updatePost(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //게시글 삭제
    @DeleteMapping("{userId}/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.successMessage("게시글이 삭제되었습니다."));
    }

    //커뮤니티 홈 - 최신글 10개
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getRecentPosts() {
        List<PostResponse> posts = postService.getRecentPosts();
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    //실시간 HOT - 오늘 날짜 + 좋아요 순 Top 5
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getTodayHotPosts() {
        List<PostResponse> posts = postService.getTodayHotPosts();
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    //게시글 검색 (제목 + 내용)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PostResponse>>> searchPosts(@RequestParam String keyword) {
        List<PostResponse> posts = postService.searchPosts(keyword);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    //상세조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long id) {
        PostResponse response = postService.getPost(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
