package sizz.api.news.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 유저의 관심 키워드 목록 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterestResponse {

    private List<TopicInterestDto> topics; // 상위 관심 키워드 목록
}
