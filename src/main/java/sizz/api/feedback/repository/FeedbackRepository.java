package sizz.api.feedback.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import sizz.api.feedback.entity.FeedbackEntity;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    // 내 피드백을 시간 역순으로 슬라이스 조회
    Slice<FeedbackEntity> findByUserIdOrderByIdDesc(String userId, Pageable pageable);
}
