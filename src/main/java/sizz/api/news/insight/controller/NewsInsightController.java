package sizz.api.news.insight.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sizz.api.news.insight.dto.InsightOverviewResponse;
import sizz.api.news.insight.dto.UserInclinationStatsResponse;
import sizz.api.news.insight.dto.UserInterestResponse;
import sizz.api.news.insight.dto.UserWeeklyWatchTimeResponse;
import sizz.api.news.insight.service.NewsInsightService;

@RestController
@RequestMapping("/api/news/insight")
@RequiredArgsConstructor
public class NewsInsightController {

    private final NewsInsightService newsInsightService;

    // NOTE: 프론트 성향 분석 페이지는 주로 /overview 사용
    // /inclination, /watch-time/weekly, /interests 는 재사용/디버깅용

    /**
     * 내 뉴스 소비 성향 통계
     * - 기본: 최근 30일 기준
     * - days 파라미터로 기간 조정 가능
     */
    @GetMapping("/inclination")
    public ResponseEntity<UserInclinationStatsResponse> getMyInclination(
            @AuthenticationPrincipal String email,
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        UserInclinationStatsResponse response =
                newsInsightService.getUserInclinationStats(email, days);
        return ResponseEntity.ok(response);
    }

    /**
     * 유저의 이번 주 / 지난 주 뉴스 시청 시간 비교
     */
    @GetMapping("/watch-time/weekly")
    public ResponseEntity<UserWeeklyWatchTimeResponse> getMyWeeklyWatchTime(
            @AuthenticationPrincipal String email
    ) {
        UserWeeklyWatchTimeResponse response = newsInsightService.getWeeklyWatchTime(email);
        return ResponseEntity.ok(response);
    }

    /**
     * 유저의 최근 관심 키워드 상위 목록
     * - 기본: 최근 30일, 상위 5개
     */
    @GetMapping("/interests")
    public ResponseEntity<UserInterestResponse> getMyInterests(
            @AuthenticationPrincipal String email,
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "limit", defaultValue = "5") int limit
    ) {
        UserInterestResponse response =
                newsInsightService.getUserInterests(email, days, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * 성향 분석 페이지용 통합 API
     * - 성향 비율 + 이번주/지난주 시청 시간 + 관심 키워드 상위 목록
     */
    @GetMapping("/overview")
    public ResponseEntity<InsightOverviewResponse> getOverview(
            @AuthenticationPrincipal String email,
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "interestLimit", defaultValue = "5") int interestLimit
    ) {
        UserInclinationStatsResponse inclination =
                newsInsightService.getUserInclinationStats(email, days);

        UserWeeklyWatchTimeResponse weeklyWatchTime =
                newsInsightService.getWeeklyWatchTime(email);

        // 최근 days일 기준, 상위 interestLimit개 관심 키워드
        UserInterestResponse interests =
                newsInsightService.getUserInterests(email, days, interestLimit);

        InsightOverviewResponse response = InsightOverviewResponse.builder()
                .inclination(inclination)
                .weeklyWatchTime(weeklyWatchTime)
                .interests(interests)
                .build();

        return ResponseEntity.ok(response);
    }

}