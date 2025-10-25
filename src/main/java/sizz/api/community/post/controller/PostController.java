package sizz.api.community.post.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    // 게시글 작성
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal String email,
            @RequestPart("data") PostRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        if (email == null) return ResponseEntity.status(401).build();
        PostResponse response = postService.createPost(email, request, image);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 수정
    @PutMapping(value = "/{postId}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal String email,
            @PathVariable Long postId,
            @RequestPart("data") PostRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        if (email == null) return ResponseEntity.status(401).build();
        PostResponse response = postService.updatePost(email, postId, request, image);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal String email,
            @PathVariable Long postId
    ) {
        if (email == null) return ResponseEntity.status(401).build();
        postService.deletePost(email, postId);
        return ResponseEntity.ok(ApiResponse.successMessage("게시글이 삭제되었습니다."));
    }

    //커뮤니티 홈 - 최신글 10개
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getRecentPosts(@AuthenticationPrincipal String email) {
        List<PostResponse> posts = postService.getRecentPosts(email);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    //실시간 HOT - 오늘 날짜 + 좋아요 순 Top 5
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getTodayHotPosts(@AuthenticationPrincipal String email) {
        List<PostResponse> posts = postService.getTodayHotPosts(email);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    //게시글 검색 (제목 + 내용)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PostResponse>>> searchPosts(
            @AuthenticationPrincipal String email,
            @RequestParam String keyword
    ) {
        List<PostResponse> posts = postService.searchPosts(email, keyword);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    //상세조회
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @AuthenticationPrincipal String email,
            @PathVariable Long postId
    ) {
        PostResponse response = postService.getPost(email, postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
