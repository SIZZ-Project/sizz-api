package sizz.api.news.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저의 뉴스 성향 통계 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInclinationStatsResponse {

    // 총 본 뉴스 수 (기간 내)
    private long totalViews;

    // 비율(0 ~ 1 사이 값)
    private double progressiveRatio;
    private double conservativeRatio;
    private double neutralRatio;
}
