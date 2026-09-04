package com.team2.postservice.post.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();

    @Override
    public StoredFile store(MultipartFile file) {
        try {
            // 상위 디렉토리가 없으면 한 번에 생성
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID() + "_" + originalFilename;
            Path destPath = uploadPath.resolve(storedFileName);

            file.transferTo(destPath.toFile());

            String imageUrl = "http://localhost:8082/images/" + storedFileName;
            return new StoredFile(imageUrl, storedFileName);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    @Override
    public void delete(String storedFileName) {
        try {
            Path filePath = uploadPath.resolve(storedFileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제에 실패했습니다.", e);
        }
    }
}