package sizz.api.news.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sizz.api.news.dto.NewsDto;
import sizz.api.news.entity.NewsDocument;
import sizz.api.news.repository.NewsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSyncService {

    private final NewsRepository newsRepository;

    public int syncNews(List<NewsDto> articles) {
        int saved = 0;

        for(NewsDto article : articles) {

            try{
                String articleId = article.getArticleId();
                if (articleId == null || articleId.isBlank()) {
                    log.warn("articleId가 비어있어서 저장 스킵 (title='{}')", article.getTitle());
                    continue;
                }
                if (newsRepository.existsByArticleId(articleId)) {
                    continue;
                }

                NewsDocument doc = NewsDocument.fromDto(article);
                newsRepository.save(doc);
                saved++;

            } catch(Exception e){
                log.error("뉴스 저장 오류 articleId={} msg={}", article.getArticleId(), e.getMessage(), e);
            }

        }

        return saved;

    }

}
