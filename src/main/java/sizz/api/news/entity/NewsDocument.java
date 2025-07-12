package sizz.api.news.entity;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import sizz.api.news.dto.NewsDto;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDocument {

    @Id
    private String id;

    private String articleId;
    private String title;
    private String description;
    private String link;
    private String imageUrl;
    private String sourceId;
    private String sourceName;
    private List<String> keywords;
    private List<String> category;
    private LocalDateTime pubDate;
    private Long viewCount;

    public static NewsDocument fromDto(NewsDto dto) {
        return NewsDocument.builder()
                .articleId(dto.getArticleId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .link(dto.getLink())
                .imageUrl(dto.getImageUrl())
                .sourceId(dto.getSourceId())
                .sourceName(dto.getSourceName())
                .keywords(dto.getKeywords())
                .category(dto.getCategory())
                .pubDate(dto.getPubDate())
                .viewCount(dto.getViewCount())
                .build();
    }

}
