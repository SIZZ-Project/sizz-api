package sizz.api.news.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 이번 주 vs 지난 주 뉴스 시청 시간 비교 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWeeklyWatchTimeResponse {

    // 이번 주 총 시청 시간(초)
    private long thisWeekSeconds;

    // 지난 주 총 시청 시간(초)
    private long lastWeekSeconds;

    // 이번 주 - 지난 주 차이(초) (양수면 증가, 음수면 감소)
    private long diffSeconds;

    // 이번 주 날짜별 시청 시간 (라인 차트용)
    private List<WatchTimePointDto> thisWeekTimeline;

    // 지난 주 날짜별 시청 시간 (라인 차트용)
    private List<WatchTimePointDto> lastWeekTimeline;
}