package sizz.api.news.dto;

import lombok.Data;

import java.util.List;

@Data
public class NewsApiResponse {

    private List<NewsDto> results;

}
