package sizz.api.news.insight.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sizz.api.news.entity.NewsDocument;
import sizz.api.news.insight.document.UserArticleLogDocument;
import sizz.api.news.insight.dto.*;
import sizz.api.news.insight.repository.UserArticleLogRepository;
import sizz.api.news.repository.NewsRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsInsightService {

    private final UserArticleLogRepository userArticleLogRepository;
    private final NewsRepository newsRepository;

    /**
     * 최근 N일 기준 유저의 뉴스 성향 통계를 계산해서 반환.
     * - 로그는 Mongo(UserArticleLogDocument)
     * - 뉴스 성향은 NewsDocument.inclination 사용
     */
    public UserInclinationStatsResponse getUserInclinationStats(String email, int days) {
        if (email == null) {
            return emptyStats();
        }

        LocalDateTime from = LocalDateTime.now().minusDays(days);

        // 최근 N일간 유저가 본 뉴스 로그 조회
        List<UserArticleLogDocument> logs =
                userArticleLogRepository.findByUserEmailAndLastViewedAtAfter(email, from);

        if (logs.isEmpty()) {
            return emptyStats();
        }

        // 뉴스 ID 목록 뽑아서 한 번에 조회
        List<String> newsIds = logs.stream()
                .map(UserArticleLogDocument::getNewsId)
                .distinct()
                .collect(Collectors.toList());

        List<NewsDocument> newsList = newsRepository.findByIdIn(newsIds);

        // id -> NewsDocument 맵
        Map<String, NewsDocument> newsMap = newsList.stream()
                .collect(Collectors.toMap(NewsDocument::getId, n -> n));

        double progressiveScore = 0.0;
        double conservativeScore = 0.0;
        double neutralScore = 0.0;
        long totalViews = 0L;

        for (UserArticleLogDocument log : logs) {
            NewsDocument news = newsMap.get(log.getNewsId());
            if (news == null) {
                continue;
            }

            String inclination = news.getInclination();
            long viewCount = log.getViewCount();
            int dwellTime = (log.getDwellTimeSec() == null ? 0 : log.getDwellTimeSec());

            // 조회수 + 머문시간(초)를 조합
            double weight = viewCount + (dwellTime / 30.0); // 30초에 1점 정도

            totalViews += viewCount;

            if (inclination == null) {
                continue;
            }

            switch (inclination.toUpperCase()) {
                case "PROGRESSIVE":
                case "진보":
                    progressiveScore += weight;
                    break;
                case "CONSERVATIVE":
                case "보수":
                    conservativeScore += weight;
                    break;
                case "NEUTRAL":
                case "중립":
                    neutralScore += weight;
                    break;
                default:
                    // 기타 값은 무시
            }
        }

        double sum = progressiveScore + conservativeScore + neutralScore;
        double progressiveRatio = 0.0;
        double conservativeRatio = 0.0;
        double neutralRatio = 0.0;

        if (sum > 0) {
            progressiveRatio = progressiveScore / sum;
            conservativeRatio = conservativeScore / sum;
            neutralRatio = neutralScore / sum;
        }

        return UserInclinationStatsResponse.builder()
                .totalViews(totalViews)
                .progressiveRatio(progressiveRatio)
                .conservativeRatio(conservativeRatio)
                .neutralRatio(neutralRatio)
                .build();
    }

    private UserInclinationStatsResponse emptyStats() {
        return UserInclinationStatsResponse.builder()
                .totalViews(0L)
                .progressiveRatio(0.0)
                .conservativeRatio(0.0)
                .neutralRatio(0.0)
                .build();
    }

    /**
     * 유저의 이번 주 / 지난 주 시청 시간 비교
     * - 기준: 월요일 ~ 일요일
     * - dwellTimeSec(총 머문 시간)을 lastViewedAt 날짜 기준으로 합산
     */
    public UserWeeklyWatchTimeResponse getWeeklyWatchTime(String email) {
        if (email == null) {
            return emptyWeekly();
        }

        LocalDate today = LocalDate.now();

        // 이번 주 월요일
        LocalDate startOfThisWeek = today.with(DayOfWeek.MONDAY);
        // 지난 주 월요일
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        // 지난 주 일요일
        LocalDate endOfLastWeek = startOfThisWeek.minusDays(1);

        // 지난 주 월요일 0시부터의 로그만 조회하면,
        // 지난 주 + 이번 주 데이터가 전부 포함됨
        LocalDateTime from = startOfLastWeek.atStartOfDay();

        List<UserArticleLogDocument> logs =
                userArticleLogRepository.findByUserEmailAndLastViewedAtAfter(email, from);

        if (logs.isEmpty()) {
            return emptyWeekly();
        }

        // 날짜별 시청 시간(초) 합산
        Map<LocalDate, Long> secondsPerDate = new HashMap<>();

        for (UserArticleLogDocument log : logs) {
            Integer dwell = log.getDwellTimeSec();
            if (dwell == null || dwell <= 0) {
                continue;
            }

            LocalDate date = log.getLastViewedAt().toLocalDate();
            // 지난 주 ~ 이번 주 범위 안에 있는 날짜만 사용
            if (date.isBefore(startOfLastWeek) || date.isAfter(today)) {
                continue;
            }

            secondsPerDate.merge(date, (long) dwell, Long::sum);
        }

        // --- 이번 주 합산 + 타임라인 ---
        long thisWeekSeconds = 0L;
        List<WatchTimePointDto> thisWeekTimeline = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfThisWeek.plusDays(i);
            if (date.isAfter(today)) break; // 아직 오지 않은 날짜는 제외

            long sec = secondsPerDate.getOrDefault(date, 0L);
            thisWeekSeconds += sec;
            thisWeekTimeline.add(
                    WatchTimePointDto.builder()
                            .date(date)
                            .seconds(sec)
                            .build()
            );
        }

        // --- 지난 주 합산 + 타임라인 ---
        long lastWeekSeconds = 0L;
        List<WatchTimePointDto> lastWeekTimeline = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfLastWeek.plusDays(i);
            if (date.isAfter(endOfLastWeek)) break;

            long sec = secondsPerDate.getOrDefault(date, 0L);
            lastWeekSeconds += sec;
            lastWeekTimeline.add(
                    WatchTimePointDto.builder()
                            .date(date)
                            .seconds(sec)
                            .build()
            );
        }

        long diff = thisWeekSeconds - lastWeekSeconds;

        return UserWeeklyWatchTimeResponse.builder()
                .thisWeekSeconds(thisWeekSeconds)
                .lastWeekSeconds(lastWeekSeconds)
                .diffSeconds(diff)
                .thisWeekTimeline(thisWeekTimeline)
                .lastWeekTimeline(lastWeekTimeline)
                .build();
    }

    private UserWeeklyWatchTimeResponse emptyWeekly() {
        return UserWeeklyWatchTimeResponse.builder()
                .thisWeekSeconds(0L)
                .lastWeekSeconds(0L)
                .diffSeconds(0L)
                .thisWeekTimeline(Collections.emptyList())
                .lastWeekTimeline(Collections.emptyList())
                .build();
    }

    /**
     * 최근 N일 기준 유저의 관심 키워드 상위 목록 조회
     * - 뉴스의 keywords를 기준으로, 같은 키워드는 점수를 합산
     * - 점수 = viewCount + dwellTimeSec / 30.0
     * - 상위 limit개만 반환
     */
    public UserInterestResponse getUserInterests(String email, int days, int limit) {
        if (email == null) {
            return emptyInterests();
        }

        LocalDateTime from = LocalDateTime.now().minusDays(days);

        // 최근 N일 간 본 뉴스 로그
        List<UserArticleLogDocument> logs =
                userArticleLogRepository.findByUserEmailAndLastViewedAtAfter(email, from);

        if (logs.isEmpty()) {
            return emptyInterests();
        }

        // 로그에 등장한 뉴스 ID 모아서 한 번에 조회
        List<String> newsIds = logs.stream()
                .map(UserArticleLogDocument::getNewsId)
                .distinct()
                .collect(Collectors.toList());

        List<NewsDocument> newsList = newsRepository.findByIdIn(newsIds);

        Map<String, NewsDocument> newsMap = newsList.stream()
                .collect(Collectors.toMap(NewsDocument::getId, n -> n));

        // 키워드별 점수
        Map<String, Double> keywordScores = new HashMap<>();

        for (UserArticleLogDocument log : logs) {
            NewsDocument news = newsMap.get(log.getNewsId());
            if (news == null) continue;

            // 뉴스에 달린 키워드들
            List<String> keywords = news.getKeywords();
            if (keywords == null || keywords.isEmpty()) continue;

            long viewCount = log.getViewCount();
            int dwellTime = (log.getDwellTimeSec() == null ? 0 : log.getDwellTimeSec());

            // 조회수 + 머문 시간 기반 가중치
            double weight = viewCount + (dwellTime / 30.0);
            if (weight <= 0) continue;

            for (String rawKeyword : keywords) {
                if (rawKeyword == null) continue;
                String keyword = rawKeyword.trim();
                if (keyword.isEmpty()) continue;

                double current = keywordScores.getOrDefault(keyword, 0.0);
                keywordScores.put(keyword, current + weight);
            }
        }

        if (keywordScores.isEmpty()) {
            return emptyInterests();
        }

        // 점수 높은 순으로 정렬 후 상위 limit개만
        List<Map.Entry<String, Double>> sorted = keywordScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        double topSum = sorted.stream()
                .mapToDouble(Map.Entry::getValue)
                .sum();

        List<TopicInterestDto> topics = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sorted) {
            double score = entry.getValue();
            double ratio = (topSum > 0) ? score / topSum : 0.0;

            topics.add(TopicInterestDto.builder()
                    .keyword(entry.getKey())
                    .score(score)
                    .ratio(ratio)
                    .build());
        }

        return UserInterestResponse.builder()
                .topics(topics)
                .build();
    }

    private UserInterestResponse emptyInterests() {
        return UserInterestResponse.builder()
                .topics(Collections.emptyList())
                .build();
    }

}
