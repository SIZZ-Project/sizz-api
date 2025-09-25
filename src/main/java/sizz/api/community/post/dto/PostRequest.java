package sizz.api.community.post.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequest {

    private String userId;
    private String title;
    private String content;
    private String imageUrl; // 이미지 첨부 시 사용
}
