package sizz.api.reaction.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import sizz.api.reaction.dto.ReactionType;

import java.time.Instant;

@Document(collection = "news_reactions")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_user_article", def = "{'userId': 1, 'articleId': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsReactionDocument {

    @Id
    private String id;

    private String userId;
    private String articleId;
    private ReactionType reaction;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
