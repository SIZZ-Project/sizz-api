package sizz.api.core.pagination;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class CursorPage<T> {
    private final List<T> items;
    private final String nextCursor;    // 마지막 요소 기준 커서
    private final boolean hasNext;      // 다음 페이지 여부
    private final String prevCursor;    // 첫 요소 기준 커서
    private final boolean hasPrev;      // 이전 페이지 여부
}