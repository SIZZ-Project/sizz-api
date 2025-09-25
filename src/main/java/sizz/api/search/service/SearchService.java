package sizz.api.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import sizz.api.core.pagination.CursorPage;
import sizz.api.news.entity.NewsDocument;
import sizz.api.search.dto.SearchNewsResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static sizz.api.news.service.CursorUtils.run;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final MongoTemplate mongo;

    public Page<SearchNewsResponseDto> searchNewsPage(String query, Pageable pageable) {
        if (query == null || query.isBlank()) return Page.empty(pageable);

        List<String> tokens = KoreanTokenizer.extractNounPhrases(query);
        if (tokens.isEmpty()) return Page.empty(pageable);

        List<Criteria> ors = tokens.stream()
                .flatMap(t -> {
                    String e = Pattern.quote(t);
                    return Stream.of(
                            Criteria.where("title").regex(e, "i"),
                            Criteria.where("description").regex(e, "i")
                    );
                })
                .toList();

        Query q = new Query(new Criteria().orOperator(ors.toArray(new Criteria[0]))).with(pageable);

        List<NewsDocument> docs = mongo.find(q, NewsDocument.class);
        long total = mongo.count(Query.of(q).limit(-1).skip(-1), NewsDocument.class);

        List<SearchNewsResponseDto> content = docs.stream()
                .map(SearchNewsResponseDto::from)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public CursorPage<SearchNewsResponseDto> searchNewsCursor(String q, Integer limit, String after, String before) {

        Criteria keyword = new Criteria();

        if (q != null && !q.isBlank()) {
            List<String> tokens = KoreanTokenizer.extractNounPhrases(q);
            if (tokens.isEmpty()) {
                return new CursorPage<>(List.of(), null, false, null, false);
            }
            List<Criteria> orList = new ArrayList<>();

            for(String token : tokens) {
                String escaped = Pattern.quote(token);
                orList.add(Criteria.where("title").regex(escaped, "i"));
                orList.add(Criteria.where("description").regex(escaped, "i"));
            }

            keyword = new Criteria().orOperator(orList.toArray(new Criteria[0]));
        }
        return run(mongo, keyword, limit, after, before, SearchNewsResponseDto::from);
    }

}
