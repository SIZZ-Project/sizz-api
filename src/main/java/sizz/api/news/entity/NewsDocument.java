package sizz.api.news.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import sizz.api.news.dto.NewsDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Document(collection = "news")
@CompoundIndexes({
        @CompoundIndex(name="pubDate__id_desc", def="{'pubDate': -1, '_id': -1}"),
        @CompoundIndex(name="viewCount_pubDate__id_desc", def="{'viewCount': -1, 'pubDate': -1, '_id': -1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String articleId;

    private String title;
    private String summary;
    private String link;
    private String imageUrl;
    private String sourceId;
    private String sourceName;

    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @Builder.Default
    private List<String> category = new ArrayList<>();

    private LocalDateTime pubDate;

    @Builder.Default
    private long viewCount = 0L;

    private String inclination;

    public static NewsDocument fromDto(NewsDto dto) {
        return NewsDocument.builder()
                .articleId(dto.getArticleId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .link(dto.getLink())
                .imageUrl(dto.getImageUrl())
                .sourceId(dto.getSourceId())
                .sourceName(dto.getSourceName())
                .keywords(Optional.ofNullable(dto.getKeywords()).orElseGet(Collections::emptyList))
                .category(Optional.ofNullable(dto.getCategory()).orElseGet(Collections::emptyList))
                .pubDate(dto.getPubDate())
                .viewCount(dto.getViewCount() == null ? 0L : dto.getViewCount())
                .inclination(dto.getInclination())
                .build();
    }
}
