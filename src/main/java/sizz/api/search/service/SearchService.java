package sizz.api.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sizz.api.news.repository.NewsRepository;
import sizz.api.search.dto.SearchNewsResponseDto;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final NewsRepository newsRepository;

    public Page<SearchNewsResponseDto> searchNews(String query, Pageable pageable) {
        return newsRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable)
                .map(SearchNewsResponseDto::from);
    }

}
