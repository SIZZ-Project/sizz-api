package sizz.api.news.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
public class NewsDto {

    private String articleId;
    private String title;
    private String description;
    private String link;
    private String imageUrl;
    private List<String> keywords;
    private List<String> category;

}
