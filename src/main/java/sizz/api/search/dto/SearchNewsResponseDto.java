package sizz.api.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sizz.api.news.entity.NewsDocument;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SearchNewsResponseDto {

    private String articleId;
    private String title;
    private String summary;
    private String link;
    private LocalDateTime pubDate;
    private String sourceName;

    public static SearchNewsResponseDto from(NewsDocument news) {
        return new SearchNewsResponseDto(
                news.getArticleId(),
                news.getTitle(),
                news.getSummary(),
                news.getLink(),
                news.getPubDate(),
                news.getSourceName()
        );
    }

}
