package sizz.api.community.post.entity;

import jakarta.persistence.*;
import lombok.*;
import sizz.api.community.common.BaseEntity;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //유저 아이디
    @Column(nullable = false)
    private String userId;

    // 게시글 제목
    @Column(nullable = false, length = 200)
    private String title;

    // 게시글 본문
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 게시글 이미지 (선택)
    private String imageUrl;

    // 좋아요 수
    @Builder.Default
    @Column(nullable = false)
    private int likeCount = 0;

    // 댓글 수
    @Builder.Default
    @Column(nullable = false)
    private int commentCount = 0;
}
