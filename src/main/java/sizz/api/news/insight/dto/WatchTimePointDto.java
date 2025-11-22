package sizz.api.news.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 특정 날짜에 뉴스를 본 총 시청 시간(초)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchTimePointDto {

    private LocalDate date;   // 날짜
    private long seconds;     // 그날 총 시청 시간(초)
}
