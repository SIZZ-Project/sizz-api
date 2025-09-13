package sizz.api.news.service;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import sizz.api.core.pagination.CursorPage;
import sizz.api.news.entity.NewsDocument;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public final class CursorUtils {
    private CursorUtils() {}

    // 정렬 고정 (내림차순/오름차순 둘 다 준비)
    private static final Sort SORT_DESC =
            Sort.by(Sort.Order.desc("pubDate"), Sort.Order.desc("_id"));
    private static final Sort SORT_ASC =
            Sort.by(Sort.Order.asc("pubDate"), Sort.Order.asc("_id"));

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private static final String DELIM = "|";

    // ===== 공통 유틸 =====

    public static int normalizeLimit(Integer limit) {
        int t = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;
        return Math.min(t, MAX_LIMIT);
    }

    public static record CursorKey(LocalDateTime pubDate, String id) {}

    public static CursorKey parse(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] p = decoded.split("\\|", 2);
            if (p.length != 2) return null;
            return new CursorKey(LocalDateTime.parse(p[0]), p[1]);
        } catch (Exception e) {
            return null;
        }
    }

    public static String encode(LocalDateTime pubDate, String id) {
        String raw = pubDate + DELIM + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isEmpty(Criteria c) { return c == null || c.getCriteriaObject().isEmpty(); }
    private static Criteria and(Criteria a, Criteria b) {
        if (isEmpty(a)) return isEmpty(b) ? new Criteria() : b;
        if (isEmpty(b)) return a;
        return new Criteria().andOperator(a, b);
    }

    // ===== 커서 조건 =====

    private static Criteria afterCriteria(CursorKey key) {
        if (key == null) return new Criteria(); // no-op
        Object id = ObjectId.isValid(key.id()) ? new ObjectId(key.id()) : key.id();
        return new Criteria().orOperator(
                where("pubDate").lt(key.pubDate()),
                new Criteria().andOperator(
                        where("pubDate").is(key.pubDate()),
                        where("_id").lt(id)
                )
        );
    }

    private static Criteria beforeCriteria(CursorKey key) {
        if (key == null) return new Criteria(); // no-op
        Object id = ObjectId.isValid(key.id()) ? new ObjectId(key.id()) : key.id();
        return new Criteria().orOperator(
                where("pubDate").gt(key.pubDate()),
                new Criteria().andOperator(
                        where("pubDate").is(key.pubDate()),
                        where("_id").gt(id)
                )
        );
    }

    // ===== 슬라이스(+1) → 커서 계산 =====
    private static <R> CursorPage<R> toPageDesc(
            List<NewsDocument> descPlusOne, int take, Function<NewsDocument, R> mapper,
            boolean hasPrevFlag // 호출 컨텍스트에 따라 hasPrev를 더 정확히 표시
    ) {
        boolean hasNext = descPlusOne.size() > take;
        List<NewsDocument> items = hasNext ? descPlusOne.subList(0, take) : descPlusOne;

        String next = null, prev = null;
        boolean hasPrev = false;

        if (!items.isEmpty()) {
            NewsDocument first = items.get(0);
            NewsDocument last  = items.get(items.size() - 1);
            prev = encode(first.getPubDate(), first.getId()); // before=prev
            if (hasNext) next = encode(last.getPubDate(), last.getId()); // after=next
            hasPrev = hasPrevFlag || true; // 최소 prev 커서 제공 가능
        }

        return new CursorPage<>(
                items.stream().map(mapper).toList(),
                next, hasNext,
                prev, hasPrev
        );
    }

    // ===== 실행 (양방향) =====
    public static <R> CursorPage<R> run(
            MongoTemplate mongo,
            Criteria baseCriteria,
            Integer limit,
            String after,
            String before,
            Function<NewsDocument, R> mapper
    ) {
        int take = normalizeLimit(limit);
        CursorKey afterKey  = parse(after);
        CursorKey beforeKey = parse(before);

        Query q;
        boolean reversed = false;
        boolean hasPrevFlag = false;

        if (beforeKey != null) {
            Criteria cond = and(baseCriteria, beforeCriteria(beforeKey));
            q = new Query(cond).with(SORT_ASC).limit(take + 1);
            reversed = true;
        } else {
            Criteria cond = and(baseCriteria, afterCriteria(afterKey));
            q = new Query(cond).with(SORT_DESC).limit(take + 1);

            hasPrevFlag = (afterKey != null);
        }

        List<NewsDocument> raw = mongo.find(q, NewsDocument.class);
        if (reversed) {
            java.util.Collections.reverse(raw);
        }

        if (beforeKey != null) {
            hasPrevFlag = raw.size() > take;
        }

        return toPageDesc(raw, take, mapper, hasPrevFlag);
    }
}