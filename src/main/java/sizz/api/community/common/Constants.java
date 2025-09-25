package sizz.api.community.common;

public class Constants {

    private Constants() {
        throw new IllegalStateException("Utility class");
    }

    // 페이지 기본 사이즈
    //public static final int DEFAULT_PAGE_SIZE = 20;

    // 날짜 포맷 - 필요시 수정 예정
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // 공통 메시지
    public static final String SUCCESS_MESSAGE = "요청 성공";
    public static final String FAIL_MESSAGE = "요청 실패";

    // 커뮤니티 홈 - 최신글 개수
    public static final int COMMUNITY_HOME_SIZE = 10;

    // 실시간 HOT - 좋아요 상위 개수
    public static final int HOT_POST_SIZE = 5;
    
}