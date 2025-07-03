package sizz.api.news.entity;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import sizz.api.news.dto.NewsDto;

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
    private List<String> keywords;
    private List<String> category;

    public static NewsDocument fromDto(NewsDto dto) {
        return NewsDocument.builder()
                .articleId(dto.getArticleId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .link(dto.getLink())
                .imageUrl(dto.getImageUrl())
                .keywords(dto.getKeywords())
                .category(dto.getCategory())
                .build();
    }

}
