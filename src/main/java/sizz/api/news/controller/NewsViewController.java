package sizz.api.news.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sizz.api.news.service.NewsViewService;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsViewController {

    private final NewsViewService newsViewService;

    @PatchMapping("/{articleId}/view")
    public void increaseView(@PathVariable String articleId){
        newsViewService.increaseView(articleId);
    }

}
