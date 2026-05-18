# SIZZ API

> 외부 뉴스 수집 · 저장 · 조회와 사용자 성향 기반 뉴스 추천을 담당하는 백엔드 서버

SIZZ는 사용자의 뉴스 소비 성향과 반응 데이터를 바탕으로 관심 뉴스와 반대 성향 뉴스를 함께 제공해,
한쪽 시각에 치우치지 않도록 돕는 뉴스 서비스입니다.

이 저장소는 외부 뉴스 API 연동, 뉴스 수집 배치, 저장소 분리, 캐시 적용, 사용자 성향 기반 추천과 AI 기반 뉴스 인사이트 생성 등 백엔드 로직 전반을 담당합니다.

> 팀 프로젝트 종료로 운영 서버는 현재 중단된 상태입니다.  
> 백엔드 구현 내용과 설계 의도는 본 저장소를 통해 확인할 수 있습니다.

---

## 1. 기술 스택

**Backend** | Java 21 · Spring Boot · Spring Security · Spring Data JPA · Spring Data MongoDB · JWT · Redis · Spring Scheduler  
**Database** | MySQL · MongoDB · Redis  
**AI** | Gemini API  
**Infra** | AWS EC2 · GitHub Actions

---

## 2. 시스템 아키텍처

```text
[Client]
   |
   v
[Spring Boot API Server (AWS EC2)]
   |
   +-- MySQL     : 사용자 / 인증 / 게시글
   +-- MongoDB   : 뉴스 원문 / 조회 로그 / 반응 데이터
   +-- Redis     : HOT 뉴스 캐시 / 인사이트 캐시
   |
   +-- Scheduler : 외부 뉴스 수집 (3시간 주기)
                   HOT 뉴스 캐시 갱신 (5시간 주기)
                   인사이트 키워드 생성 (매일 06:00)
```

---

## 3. 왜 이렇게 설계했나

### 3-1. MySQL과 MongoDB를 분리한 이유

뉴스 데이터와 사용자 데이터는 성격이 다르다고 판단했습니다.

| 구분 | 저장소 | 이유 |
|---|---|---|
| 뉴스 원문 / 조회 로그 / 반응 데이터 | MongoDB | 외부 API 응답 구조 변화에 유연하고 문서형 저장에 적합 |
| 사용자 / 인증 / 게시글 | MySQL | 정합성과 관계형 조회가 중요하고 트랜잭션 처리가 필요 |

뉴스는 외부 API 구조가 바뀌어도 유연하게 수용할 수 있어야 했고,  
사용자 데이터는 중복 체크, 권한 확인처럼 정합성이 중요해 MySQL로 유지했습니다.

### 3-2. Redis 캐시를 적용한 이유

HOT 뉴스와 인사이트는 반복 조회가 많아 매번 DB를 조회하면 비효율적이라고 판단했습니다.

```text
HOT 뉴스 조회 흐름:
Redis 캐시 HIT  → 즉시 반환
Redis 캐시 MISS → MongoDB 조회 후 캐시 저장 (TTL 5시간)
Redis 장애      → MongoDB fallback (서비스 중단 없음)
```

Redis 장애 시 서비스가 중단되지 않도록 DB fallback 처리를 구현했습니다.

```java
try {
    base = (List<NewsResponseDto>) redisTemplate.opsForValue().get(HOT_NEWS_CACHE_KEY);
} catch (Exception e) {
    base = null;
    log.warn("Redis cache get failed. fallback to DB", e);
}

if (base == null) {
    base = refreshHotNewsCache();
}
```

### 3-3. 스케줄러로 외부 API 호출을 분리한 이유

사용자 요청마다 외부 뉴스 API를 직접 호출하면 응답 지연이나 외부 API 장애가 서비스에 바로 영향을 줍니다.  
3시간 주기 배치로 분리해 조회 API는 내부 저장소 기준으로 빠르게 응답하도록 구성했습니다.

---

## 4. 핵심 구현

### 4-1. 뉴스 수집 파이프라인

```text
외부 뉴스 API 호출
   → 기사 URL로 본문 크롤링 (실패 시 description fallback)
   → Gemini API로 요약 + 성향 분석 (진보 / 중립 / 보수)
   → articleId 중복 체크 후 MongoDB 저장
```

크롤링 실패나 Gemini 오류는 기사 단위로 독립 처리해 전체 배치가 중단되지 않도록 했습니다.  
기사 처리 간 1초 대기를 두어 외부 API 호출 부하를 제어했습니다.

### 4-2. Gemini API 재시도 전략

Gemini API는 요청이 많으면 429(Too Many Requests) 오류가 발생합니다.  
즉시 재시도하면 계속 실패하므로, 실패할수록 대기시간을 2배씩 늘려서 재시도하도록 구현했습니다.

```text
1회 실패 → 800ms 후 재시도
2회 실패 → 1600ms 후 재시도
3회 실패 → 3200ms 후 재시도
최대 3회 재시도 후 실패 시 해당 기사 스킵
```

### 4-3. MongoDB 인덱스 설계

뉴스 조회 패턴에 맞춰 복합 인덱스 2개를 적용했습니다.

```java
@CompoundIndexes({
        @CompoundIndex(name="pubDate__id_desc",
                def="{'pubDate': -1, '_id': -1}"),           // 최신순 조회
        @CompoundIndex(name="viewCount_pubDate__id_desc",
                def="{'viewCount': -1, 'pubDate': -1, '_id': -1}") // HOT 뉴스 정렬
})
```

일반 뉴스 목록은 최신순으로, HOT 뉴스는 조회수 기준으로 조회하는 패턴이 달라 각각 인덱스를 분리했습니다.  
다만 실제 부하 테스트로 성능 수치를 검증하지는 못했습니다.

### 4-4. 캐시 오염 방지

HOT 뉴스는 비로그인 사용자도 조회하므로 캐시에는 공용 뉴스 데이터만 저장했습니다.  
로그인 사용자 요청 시에는 캐시에서 뉴스를 가져온 뒤, 사용자별 반응·북마크를 별도로 조회해서 서비스 레이어에서 합쳐 반환했습니다.

```java
// 캐시 DTO를 직접 수정하지 않고 새 DTO로 조합해 반환
return base.stream()
    .map(dto -> NewsResponseDto.builder()
        .articleId(dto.getArticleId())
        .title(dto.getTitle())
        // ...기존 필드 복사
        .reactionType(myReactionMap.get(dto.getArticleId()))   // 사용자별 반응
        .bookmarked(Boolean.TRUE.equals(myBookmarkMap.get(dto.getArticleId()))) // 북마크
        .build())
    .toList();
```

---

## 5. 트러블슈팅

### 5-1. Gemini API 응답이 JSON 대신 마크다운 코드블럭으로 오는 문제

#### 상황

Gemini에 JSON만 반환하도록 프롬프트를 작성했는데, Postman으로 실제 응답을 확인해보니  
` ```json {...} ``` ` 형태로 오는 경우가 있었습니다.  
이 경우 `ObjectMapper.readTree()`가 파싱에 실패해 요약 결과가 저장되지 않았습니다.

#### 해결

응답에서 코드블럭을 제거하고 JSON 부분만 추출하는 메서드를 추가했습니다.

```java
private static String extractJson(String raw) {
    String s = raw.replace("```json", "").replace("```", "").trim();

    int objSt = s.indexOf('{');
    int objEd = s.lastIndexOf('}');
    if (objSt >= 0 && objEd > objSt) {
        return s.substring(objSt, objEd + 1).trim();
    }

    int arrSt = s.indexOf('[');
    int arrEd = s.lastIndexOf(']');
    if (arrSt >= 0 && arrEd > arrSt) {
        return s.substring(arrSt, arrEd + 1).trim();
    }

    return s;
}
```

#### 배운 점

외부 AI API는 프롬프트로 형식을 지정해도 응답이 항상 일정하지 않습니다.  
실제 응답을 직접 확인하고 파싱 로직을 방어적으로 작성해야 한다는 점을 배웠습니다.

---

### 5-2. 배치 중 Gemini API 429 오류로 이후 요약 시도가 연속 실패한 문제

#### 상황

뉴스 수집 배치에서 여러 기사를 순차적으로 처리하면서 Gemini API를 연속 호출했습니다.  
이 과정에서 429(Too Many Requests) 오류가 발생했고, 별도의 대기 없이 다음 기사 요약을 계속 시도하면서 이후 요약 요청도 연속적으로 실패하는 문제가 있었습니다.

#### 원인

Gemini API의 요청 제한에 걸린 상태에서 즉시 다음 요청을 보내면, 제한이 해제되기 전에 다시 호출하게 되어 같은 오류가 반복될 수 있었습니다.  
초기 구현에서는 호출 실패 이후 일정 시간 대기하거나 재시도 간격을 조절하는 전략이 부족했습니다.

#### 해결

429 오류 발생 시 즉시 다음 요청을 보내지 않고, 실패할수록 대기시간을 늘린 뒤 재시도하도록 수정했습니다.

```text
1회 실패 → 800ms 대기 후 재시도
2회 실패 → 1600ms 대기 후 재시도
3회 실패 → 3200ms 대기 후 재시도
최대 3회 재시도 후 실패 시 해당 기사 스킵
```

또한 재시도 한도를 초과한 경우 전체 배치를 중단하지 않고 해당 기사만 스킵하도록 처리했습니다.

#### 결과

- Gemini API 요청 제한 상황에서 즉시 재호출로 인한 연속 실패를 줄였습니다.
- 특정 기사 요약 실패가 전체 뉴스 수집 배치 중단으로 이어지지 않도록 했습니다.
- 외부 API 호출이 포함된 배치에서 재시도 간격과 부분 실패 허용이 필요하다는 점을 반영했습니다.

#### 배운 점

외부 API가 포함된 배치는 정상 응답만 가정하면 안 됩니다.  
특히 요청 제한이 있는 API는 실패 시 즉시 재호출하기보다, 일정 시간 대기 후 재시도하고 실패 범위를 개별 작업 단위로 제한해야 한다는 점을 배웠습니다.

---

### 5-3. 로그인 사용자와 비로그인 사용자의 HOT 뉴스 응답 분리

#### 상황

HOT 뉴스는 공용 Redis 캐시로 관리하는데, 로그인 사용자에게는 나의 반응(좋아요/싫어요)과 북마크 정보도 함께 반환해야 했습니다.  
캐시 DTO에 직접 반응 정보를 넣으면 비로그인 사용자 응답에도 영향을 줄 수 있었습니다.

#### 해결

캐시에는 공용 뉴스 데이터만 저장하고, 로그인 사용자 요청 시에는 사용자별 반응·북마크를 별도로 조회한 뒤 응답 단계에서 합쳐 반환했습니다.

#### 배운 점

캐시 설계는 누가 이 데이터를 공유하는지를 먼저 구분해야 합니다.  
공용 캐시와 사용자별 컨텍스트를 분리하면 캐시 효율과 데이터 정확성을 동시에 가져갈 수 있습니다.

---

## 6. 개선하고 싶은 것

- `@Scheduled` 스케줄러는 단일 인스턴스를 가정합니다. 서버를 수평 확장하면 중복 실행이 발생할 수 있어 분산 락 도입이 필요합니다.
- 배치 실패 시 모니터링 알림이 없어 수동 로그 확인에 의존합니다.
- 인덱스 적용 후 실제 부하 테스트로 성능을 수치화하지 못했습니다.

---

## 7. 실행 방법

```bash
./gradlew bootRun
```

환경 변수:

- `NEWSDATA_API_KEY` : NewsData.io API 키
- `MYSQL_PASSWORD` : MySQL 비밀번호
- `MONGODB_PASSWORD` : MongoDB 비밀번호
- `GOOGLE_CLIENT_ID` : Google OAuth2 클라이언트 ID
- `GOOGLE_CLIENT_SECRET` : Google OAuth2 클라이언트 시크릿
- `JWT_SECRET` : JWT 서명 키
- `GEMINI_API_KEY` : Gemini API 키
- `AWS_S3_BUCKET_NAME` : S3 버킷 이름
- `AWS_REGION` : AWS 리전 (기본값: ap-northeast-2)
