package sizz.api.community.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

public class FileUtils {

    private static final String UPLOAD_DIR = "uploads/images/";

    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    //파일명
    public static String saveImage(MultipartFile imageFile) throws IOException {
        if (imageFile.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 저장할 수 없습니다.");
        }

        // 원본 파일 null 예외처리
        String originalFilename = Optional.ofNullable(imageFile.getOriginalFilename())
                .orElseThrow(() -> new IllegalArgumentException("파일 이름이 존재하지 않습니다."));

        // 확장자 추출
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex == -1) {
            throw new IllegalArgumentException("확장자가 없는 파일은 업로드할 수 없습니다.");
        }
        String extension = originalFilename.substring(dotIndex);

        // UUID (고유 식별자)  생성
        String uniqueFileName = UUID.randomUUID() + extension;

        // 저장 경로 설정, 자동 설정
        Path savePath = Paths.get(UPLOAD_DIR, uniqueFileName);
        Files.createDirectories(savePath.getParent());
        imageFile.transferTo(savePath.toFile());

        return uniqueFileName; // 파일명 저장
    }

    //이미지 삭제

    public static boolean deleteImage(String fileName) {
        File file = new File(UPLOAD_DIR + fileName);
        return file.exists() && file.delete();
    }
}
