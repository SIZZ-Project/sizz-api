package sizz.api.news.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sizz.api.news.entity.NewsDocument;
import sizz.api.reaction.dto.ReactionType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 응답에서 생략
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

    // 로그인 시
    private ReactionType reactionType; // nullable
    private Boolean bookmarked;        // nullable

    // 비로그인
    public static NewsResponseDto from(NewsDocument news) {
        return NewsResponseDto.builder()
                .articleId(news.getArticleId())
                .title(news.getTitle())
                .description(news.getDescription())
                .link(news.getLink())
                .category(news.getCategory())
                .pubDate(news.getPubDate())
                .sourceName(news.getSourceName())
                .viewCount(news.getViewCount())
                .inclination(news.getInclination())
                .build();
    }

    // 로그인시
    public static NewsResponseDto fromWithEngagement(NewsDocument news,
                                               ReactionType reactionType,
                                               Boolean bookmarked) {
        return NewsResponseDto.builder()
                .articleId(news.getArticleId())
                .title(news.getTitle())
                .description(news.getDescription())
                .link(news.getLink())
                .category(news.getCategory())
                .pubDate(news.getPubDate())
                .sourceName(news.getSourceName())
                .viewCount(news.getViewCount())
                .inclination(news.getInclination())
                .reactionType(reactionType)
                .bookmarked(bookmarked)
                .build();
    }
}
