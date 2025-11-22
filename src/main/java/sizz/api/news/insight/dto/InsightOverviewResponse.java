package sizz.api.news.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 성향 분석 페이지용 전체 묶음 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightOverviewResponse {

    // 정치 성향 비율 (원 그래프용)
    private UserInclinationStatsResponse inclination;

    // 이번주 / 지난주 시청 시간 비교 (라인 그래프 + 문구용)
    private UserWeeklyWatchTimeResponse weeklyWatchTime;

    // 관심 키워드 상위 목록
    private UserInterestResponse interests;
}
