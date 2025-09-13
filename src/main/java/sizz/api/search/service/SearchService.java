package sizz.api.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import sizz.api.core.pagination.CursorPage;
import sizz.api.news.repository.NewsRepository;
import sizz.api.search.dto.SearchNewsResponseDto;

import java.util.regex.Pattern;

import static sizz.api.news.service.CursorUtils.run;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final NewsRepository newsRepository;
    private final MongoTemplate mongo;

    public Page<SearchNewsResponseDto> searchNewsPage(String query, Pageable pageable) {
        return newsRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable)
                .map(SearchNewsResponseDto::from);
    }

    public CursorPage<SearchNewsResponseDto> searchNewsCursor(String q, Integer limit, String after, String before) {
        Criteria keyword = new Criteria();
        if (q != null) q = q.trim();
        if (q != null && !q.isBlank()) {
            String escaped = Pattern.quote(q);
            keyword = new Criteria().orOperator(
                    Criteria.where("title").regex(escaped, "i"),
                    Criteria.where("description").regex(escaped, "i")
            );
        }
        return run(mongo, keyword, limit, after, before, SearchNewsResponseDto::from);
    }

}
