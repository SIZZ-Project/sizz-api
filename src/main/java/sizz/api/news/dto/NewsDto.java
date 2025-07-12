package sizz.api.news.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
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
    private String sourceId;
    private String sourceName;
    private List<String> keywords;
    private List<String> category;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pubDate;
    private Long viewCount;

}
