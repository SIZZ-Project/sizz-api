package sizz.api.global.s3;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /** 이미지 업로드: 반환값으로 S3 key(예: board/uploads/123/uuid.jpg)를 돌려줌 */
    String uploadImage(MultipartFile file, String keyPrefix);

    /** 객체 삭제 */
    void delete(String key);

    /** (private 버킷일 때) 조회용 presigned URL 생성 */
    String presignedGetUrl(String key, long minutes);
}
