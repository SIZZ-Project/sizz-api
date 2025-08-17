// src/main/java/sizz/api/news/service/CursorUtils.java
package sizz.api.news.service;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import sizz.api.news.dto.CursorPage;
import sizz.api.news.entity.NewsDocument;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

public final class CursorUtils {
    private CursorUtils() {}

    // 정렬 고정: pubDate DESC, _id DESC (인덱스: {'pubDate':-1,'_id':-1} 권장)
    private static final Sort STABLE_SORT =
            Sort.by(Sort.Order.desc("pubDate"), Sort.Order.desc("_id"));

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private static final String DELIM = "|";

    // limit 기본값/상한
    public static int normalizeLimit(Integer limit) {
        int t = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;
        return Math.min(t, MAX_LIMIT);
    }

    // 커서 키
    public static record CursorKey(LocalDateTime pubDate, String id) {}

    // cursor(Base64 "pubDate|id") → CursorKey (깨지면 null = 첫 페이지로 처리)
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

    // 커서 이후 조건: (pubDate < cPub) OR (pubDate = cPub AND _id < cId)
    public static Criteria afterCriteria(CursorKey key) {
        if (key == null) return new Criteria(); // no-op
        Object id = ObjectId.isValid(key.id()) ? new ObjectId(key.id()) : key.id();
        return new Criteria().orOperator(
                Criteria.where("pubDate").lt(key.pubDate()),
                new Criteria().andOperator(
                        Criteria.where("pubDate").is(key.pubDate()),
                        Criteria.where("_id").lt(id)
                )
        );
    }

    // nextCursor = Base64("pubDate|id")
    public static String encodeNext(LocalDateTime pubDate, String id) {
        String raw = pubDate + DELIM + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    // 슬라이스(+1) → hasNext/nextCursor 계산
    public static record SliceResult(List<NewsDocument> items, String nextCursor, boolean hasNext) {}
    public static SliceResult slice(List<NewsDocument> docs, int take) {
        boolean hasNext = docs.size() > take;
        List<NewsDocument> items = hasNext ? docs.subList(0, take) : docs;
        String next = (hasNext && !items.isEmpty())
                ? encodeNext(items.get(items.size() - 1).getPubDate(), items.get(items.size() - 1).getId())
                : null;
        return new SliceResult(List.copyOf(items), next, hasNext);
    }

    private static boolean isEmpty(Criteria c) { return c == null || c.getCriteriaObject().isEmpty(); }
    private static Criteria combineAnd(Criteria base, Criteria after) {
        if (isEmpty(base))  return isEmpty(after) ? new Criteria() : after;
        if (isEmpty(after)) return base;
        return new Criteria().andOperator(base, after);
    }

    // 실행: (기본조건 AND after) → find → slice → map → CursorPage
    public static <R> CursorPage<R> run(
            MongoTemplate mongo,
            Criteria baseCriteria,
            Integer limit,
            String cursor,
            Function<NewsDocument, R> mapper
    ) {
        int take = normalizeLimit(limit);
        var key = parse(cursor);
        Criteria criteria = combineAnd(baseCriteria, afterCriteria(key));

        Query q = new Query(criteria).with(STABLE_SORT).limit(take + 1);
        List<NewsDocument> docs = mongo.find(q, NewsDocument.class);

        var slice = slice(docs, take);
        List<R> items = slice.items().stream().map(mapper).toList();
        return new CursorPage<>(items, slice.nextCursor(), slice.hasNext());
    }
}