package sizz.api.news.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sizz.api.news.dto.CursorPage;
import sizz.api.news.dto.NewsResponseDto;
import sizz.api.news.service.NewsQueryService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NewsQueryController {

    private final NewsQueryService newsQueryService;

    @GetMapping("/news/all")
    public Page<NewsResponseDto> findAllNewsPage(@PageableDefault(size = 10, sort = {"pubDate","id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return newsQueryService.findAllNewsPage(pageable);
    }

    @GetMapping("/news/hot")
    public List<NewsResponseDto> getHotNews() {
        return newsQueryService.getHotNews();
    }

    @GetMapping("/news/all-cursor")
    public CursorPage<NewsResponseDto> findAllNewsCursor(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String cursor
    ) {
        return newsQueryService.findAllNewsCursor(limit, cursor);
    }

}
