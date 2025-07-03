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

    public void syncNews(List<NewsDto> articles) {

        for(NewsDto article : articles) {

            try{
                if(newsRepository.existsByArticleId(article.getArticleId())) {
                    continue;
                }

                NewsDocument doc = NewsDocument.fromDto(article);

                newsRepository.save(doc);

            } catch(Exception e){
                log.error("뉴스 저장 오류 article id {}", article.getArticleId(), e);
            }

        }

    }

}
