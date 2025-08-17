package sizz.api.news.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class CursorPage<T> {
    private final List<T> items;
    private final String nextCursor;
    private final boolean hasNext;
}