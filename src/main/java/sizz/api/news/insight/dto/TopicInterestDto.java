package sizz.api.news.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관심 키워드 하나에 대한 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicInterestDto {

    private String keyword; // 키워드 이름

    private double score;   // 가중치 점수 (viewCount + dwellTime 기반)

    private double ratio;   // 상위 N개 안에서의 비율 (0.0 ~ 1.0)
}
