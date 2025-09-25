package sizz.api.community.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    BOARD_NOT_FOUND("BOARD_404", HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND("COMMENT_404", HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    USER_UNAUTHORIZED("USER_401", HttpStatus.UNAUTHORIZED, "권한이 없습니다.");

    private final String code;      // 고유 코드 문자열
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
