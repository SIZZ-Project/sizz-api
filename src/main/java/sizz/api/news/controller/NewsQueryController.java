package sizz.api.news.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sizz.api.core.pagination.CursorPage;
import sizz.api.news.dto.NewsResponseDto;
import sizz.api.news.service.NewsQueryService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NewsQueryController {

    private final NewsQueryService newsQueryService;

    @GetMapping("/news/all")
    public Page<NewsResponseDto> findAllNewsPage(
            @AuthenticationPrincipal String email,
            @PageableDefault(size = 10, sort = {"pubDate","id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return newsQueryService.findAllNewsPage(email, pageable);
    }

    @GetMapping("/news/hot")
    public List<NewsResponseDto> getHotNews(
            @AuthenticationPrincipal String email
    ) {
        return newsQueryService.getHotNews(email);
    }

    @GetMapping("/news/all-cursor")
    public CursorPage<NewsResponseDto> findAllNewsCursor(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before
    ) {
        return newsQueryService.findAllNewsCursor(email, limit, after, before);
    }

}
