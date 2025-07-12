package sizz.api.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sizz.api.search.dto.SearchNewsResponseDto;
import sizz.api.search.service.SearchService;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/news")
    public Page<SearchNewsResponseDto> searchNews(@RequestParam String query,
                                                  @PageableDefault(size = 10, sort = "pubDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return searchService.searchNews(query, pageable);
    }


}
