package sizz.api.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Override
    public String uploadImage(MultipartFile file, String keyPrefix) {
        try {
            String ext = getExt(file.getOriginalFilename());
            String key = keyPrefix + "/" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(put, RequestBody.fromBytes(file.getBytes()));
            return key; // DB에는 key만 저장
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket).key(key).build());
    }

    @Override
    public String presignedGetUrl(String key, long minutes) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket).key(key).build();
        GetObjectPresignRequest pre = GetObjectPresignRequest.builder()
                .getObjectRequest(get)
                .signatureDuration(Duration.ofMinutes(minutes))
                .build();
        URL url = s3Presigner.presignGetObject(pre).url();
        return url.toString();
    }

    private String getExt(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }
}
