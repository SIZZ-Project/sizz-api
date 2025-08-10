package sizz.api.news.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsDto {
    @JsonProperty("article_id")
    private String articleId;
    private String title;
    private String description;
    private String link;
    @JsonProperty("image_url")
    private String imageUrl;
    @JsonProperty("source_id")
    private String sourceId;
    @JsonProperty("source_name")
    private String sourceName;
    private List<String> keywords;
    private List<String> category;

    @JsonProperty("pubDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pubDate;

    private Long viewCount;

    private String inclination;
}
