package sizz.api.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sizz.api.feedback.dto.FeedbackRequest;
import sizz.api.feedback.dto.FeedbackResponse;
import sizz.api.feedback.entity.FeedbackEntity;
import sizz.api.feedback.repository.FeedbackRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    // 피드백 저장
    public void saveFeedback(String userId, FeedbackRequest req) {
        // 공백만 입력된 경우 방지
        String content = req.getContent() == null ? "" : req.getContent().trim();
        if (content.isEmpty()) {
            // 필요 시 예외 던지기 or 무시
            throw new IllegalArgumentException("내용을 입력해 주세요.");
        }

        FeedbackEntity entity = FeedbackEntity.builder()
                .userId(userId)
                .content(content)
                .build();

        feedbackRepository.save(entity);
    }

    // 내 피드백 목록(슬라이스) 조회
    @Transactional(readOnly = true)
    public Slice<FeedbackResponse> getMyFeedbackSlice(String userId, Pageable pageable) {
        Slice<FeedbackEntity> slice = feedbackRepository.findByUserIdOrderByIdDesc(userId, pageable);
        List<FeedbackResponse> items = slice.getContent().stream()
                .map(e -> new FeedbackResponse(e.getId(), e.getContent(), e.getCreatedAt()))
                .toList();
        return new SliceImpl<>(items, pageable, slice.hasNext());
    }
}
