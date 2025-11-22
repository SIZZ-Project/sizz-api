package sizz.api.news.insight.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sizz.api.news.insight.service.NewsViewLogService;

@RestController
@RequestMapping("/api/news/logs")
@RequiredArgsConstructor
public class NewsViewLogController {

    private final NewsViewLogService newsViewLogService;

    /**
     * 뉴스 카드/링크를 클릭해서 "봤다" 라는 이벤트를 기록.
     * - 로그인 유저만 기록함 (비로그인은 무시)
     */
    @PostMapping("/{newsId}/view")
    public ResponseEntity<Void> logNewsView(
            @AuthenticationPrincipal String email,
            @PathVariable String newsId
    ) {
        newsViewLogService.logNewsView(email, newsId);
        return ResponseEntity.ok().build();
    }

    /**
     * 뉴스 페이지에서 머문 시간을 누적하는 API.
     * - 페이지를 떠날 때 프론트에서 dwellTimeSec(초)을 보내줌.
     */
    @PostMapping("/{newsId}/dwell")
    public ResponseEntity<Void> addDwellTime(
            @AuthenticationPrincipal String email,
            @PathVariable String newsId,
            @RequestBody int dwellTimeSec
    ) {
        newsViewLogService.addDwellTime(email, newsId, dwellTimeSec);
        return ResponseEntity.ok().build();
    }

}
