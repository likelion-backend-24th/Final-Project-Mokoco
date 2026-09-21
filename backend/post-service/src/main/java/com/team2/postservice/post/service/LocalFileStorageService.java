package com.team2.postservice.post.service;

import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import org.apache.tika.Tika;
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

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final Path root;
    private final String baseUrl;

    public LocalFileStorageService(@Value("${file.upload-dir}") String uploadDir,
                                    @Value("${file.base-url}") String baseUrl) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.IMAGE_FILE_TOO_LARGE);
        }

        String mime;
        try (var input = file.getInputStream()) {
            mime = new Tika().detect(input);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        if (!ALLOWED_MIME_TYPES.contains(mime)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        String storedFileName = UUID.randomUUID() + extensionFor(mime);
        try {
            Files.createDirectories(root);
            try (var input = file.getInputStream()) {
                Files.copy(input, root.resolve(storedFileName));
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        return new StoredFile(baseUrl + "/" + storedFileName, storedFileName);
    }

    @Override
    public void delete(String storedFileName) {
        try {
            Files.deleteIfExists(root.resolve(storedFileName));
        } catch (IOException ignored) {
            // best-effort cleanup; a stray file on disk is not worth failing the request
        }
    }

    private String extensionFor(String mime) {
        return switch (mime.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
