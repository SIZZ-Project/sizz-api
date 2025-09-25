package sizz.api.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import sizz.api.core.pagination.CursorPage;
import sizz.api.search.dto.InsightDto;
import sizz.api.search.dto.SearchNewsResponseDto;
import sizz.api.search.service.InsightCacheService;
import sizz.api.search.service.SearchService;

import java.util.Optional;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final InsightCacheService insightCacheService;

    @GetMapping("/newsPage")
    public Page<SearchNewsResponseDto> searchNews(@RequestParam String query,
                                                  @PageableDefault(size = 10, sort = {"pubDate","id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return searchService.searchNewsPage(query, pageable);
    }

    @GetMapping("/newsCursor")
    public CursorPage<SearchNewsResponseDto> searchNewsCursor(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before
    ) {
        return searchService.searchNewsCursor(q, limit, after, before);
    }

    @GetMapping("/newsInsight/{field}")
    public Optional<InsightDto> getFieldInsight(@PathVariable String field) {
        return insightCacheService.find(field);
    }


}
