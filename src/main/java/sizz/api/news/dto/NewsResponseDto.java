package sizz.api.news.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import sizz.api.news.entity.NewsDocument;
import sizz.api.reaction.dto.ReactionType;

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
    private String inclination;
    private ReactionType reactionType;

    public static NewsResponseDto from(NewsDocument news, ReactionType reactionType) {
        return new NewsResponseDto(
                news.getArticleId(),
                news.getTitle(),
                news.getDescription(),
                news.getLink(),
                news.getCategory(),
                news.getPubDate(),
                news.getSourceName(),
                news.getViewCount(),
                news.getInclination(),
                reactionType
        );
    }

}
