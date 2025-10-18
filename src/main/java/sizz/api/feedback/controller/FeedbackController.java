package sizz.api.feedback.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sizz.api.feedback.dto.FeedbackRequest;
import sizz.api.feedback.dto.FeedbackResponse;
import sizz.api.feedback.service.FeedbackService;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    // 피드백 등록
    @PostMapping
    public ResponseEntity<Void> submitFeedback(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody FeedbackRequest request
    ) {
        if (email == null) return ResponseEntity.status(401).build();
        feedbackService.saveFeedback(email, request);
        return ResponseEntity.ok().build();
    }

    // 내가 남긴 피드백 목록 보기 – 무한 스크롤
    @GetMapping("/me")
    public ResponseEntity<Slice<FeedbackResponse>> getMyFeedback(
            @AuthenticationPrincipal String email,
            Pageable pageable
    ) {
        if (email == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(feedbackService.getMyFeedbackSlice(email, pageable));
    }
}
