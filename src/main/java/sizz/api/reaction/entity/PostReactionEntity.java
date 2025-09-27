package sizz.api.reaction.entity;

import jakarta.persistence.*;
import lombok.*;
import sizz.api.reaction.dto.ReactionType;

@Entity
@Getter @Setter
@Table(
        name = "posts_reactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_user_post", columnNames = {"userId", "postId"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType reaction;
}
