package com.team2.postservice.post.service;

import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    private final Path uploadDir;
    private final String baseUrl;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir,
                               @Value("${file.base-url}") String baseUrl) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("이미지 업로드 디렉토리를 생성할 수 없습니다: " + this.uploadDir, e);
        }
    }

    public StoredFile store(MultipartFile file) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path targetPath = uploadDir.resolve(storedFileName).normalize();

        if (!targetPath.getParent().equals(uploadDir)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        try {
            file.transferTo(targetPath);
        } catch (IOException e) {
            log.error("이미지 파일 저장 실패: {}", storedFileName, e);
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        String imageUrl = baseUrl + "/" + storedFileName;
        return new StoredFile(storedFileName, imageUrl);
    }

    public void delete(String storedFileName) {
        try {
            Files.deleteIfExists(uploadDir.resolve(storedFileName).normalize());
        } catch (IOException e) {
            log.warn("이미지 파일 삭제 실패: {}", storedFileName, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new CustomException(ErrorCode.IMAGE_FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
        return extension;
    }

    public record StoredFile(String storedFileName, String imageUrl) {}
}
