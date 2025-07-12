package sizz.api.news.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import sizz.api.news.entity.NewsDocument;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class NewsResponseDto {

    private String articleId;
    private String title;
    private String description;
    private String link;
    private List<String> category;
    private LocalDateTime pubDate;
    private String sourceName;
    private Long viewCount;

    public static NewsResponseDto from(NewsDocument news) {
        return new NewsResponseDto(
                news.getArticleId(),
                news.getTitle(),
                news.getDescription(),
                news.getLink(),
                news.getCategory(),
                news.getPubDate(),
                news.getSourceName(),
                news.getViewCount()
        );
    }

}
