package sizz.api.news.insight.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_article_log")
@CompoundIndexes({
        @CompoundIndex(name = "user_news_idx", def = "{'userId': 1, 'newsId': 1}", unique = true),
        @CompoundIndex(name = "user_lastViewed_idx", def = "{'userId': 1, 'lastViewedAt': -1}")
})
public class UserArticleLogDocument {

    @Id
    private String id;

    private String userEmail;

    // MongoDB news._id (NewsDocument.id)
    private String newsId;

    // 처음 본 시각
    private LocalDateTime firstViewedAt;

    // 마지막으로 본 시각
    private LocalDateTime lastViewedAt;

    // 페이지 머문 시간(초)
    private Integer dwellTimeSec;

    // 몇 번 봤는지
    private long viewCount;
}
