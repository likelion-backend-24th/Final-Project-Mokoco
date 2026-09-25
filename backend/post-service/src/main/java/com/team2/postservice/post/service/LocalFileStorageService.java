package com.team2.postservice.post.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    // 휴대폰 카메라 사진은 보통 한 변이 3000~4000px에 수 MB인데, 글 목록/상세 어디서도
    // 그렇게 큰 원본이 필요 없다. 지금까지는 업로드된 원본을 그대로 저장·서빙해서, 사진이
    // 많은 글 목록을 열 때마다 불필요하게 큰 파일을 여러 장 내려받아 느리게 느껴졌다 —
    // 업로드 시점에 한 번만 줄이고 재압축해두면 이후 모든 조회에서 전송량이 크게 준다.
    private static final int MAX_DIMENSION = 1600;
    private static final float JPEG_QUALITY = 0.85f;

    private final Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();

    @Override
    public StoredFile store(MultipartFile file) {
        // Preserve the existing post-image limit after enabling larger chat videos.
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "게시글 이미지는 10MB 이하여야 합니다.");
        }
        try {
            // 상위 디렉토리가 없으면 한 번에 생성
            Files.createDirectories(uploadPath);

            byte[] reencoded = reencodeAsJpeg(file);
            String storedFileName = reencoded != null
                    ? UUID.randomUUID() + ".jpg"
                    : UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path destPath = uploadPath.resolve(storedFileName);

            if (reencoded != null) {
                Files.write(destPath, reencoded);
            } else {
                // 디코딩할 수 없는 파일(지원하지 않는 형식 등)은 업로드 자체를 막지 않고 원본 그대로 저장한다.
                file.transferTo(destPath.toFile());
            }

            // 상대 경로로 저장한다. 브라우저는 같은 오리진(https)으로 요청 -> Caddy -> gateway(/api 제거) -> post-service /images/**
            String imageUrl = "/api/images/" + storedFileName;
            return new StoredFile(imageUrl, storedFileName);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    private byte[] reencodeAsJpeg(MultipartFile file) {
        try (var in = new MemoryCacheImageInputStream(file.getInputStream())) {
            Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) return null;
            var reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                // 매우 큰 픽셀 수의 디코딩은 240MB 힙 컨테이너에서 메모리 부담이 크므로 방어적으로 제한한다.
                if (width < 1 || height < 1 || (long) width * height > 40_000_000L) return null;
                BufferedImage original = reader.read(0);
                double ratio = Math.min(1.0, (double) MAX_DIMENSION / Math.max(width, height));
                int targetW = Math.max(1, (int) Math.round(width * ratio));
                int targetH = Math.max(1, (int) Math.round(height * ratio));
                BufferedImage target = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
                var g = target.createGraphics();
                try {
                    // JPEG는 투명도가 없으니, 투명 배경(PNG 등)은 흰 배경으로 깔고 그 위에 그린다.
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, targetW, targetH);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(original, 0, 0, targetW, targetH, null);
                } finally {
                    g.dispose();
                    original.flush();
                }
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                try {
                    ImageWriteParam params = writer.getDefaultWriteParam();
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(JPEG_QUALITY);
                    var bytes = new ByteArrayOutputStream();
                    try (var out = new MemoryCacheImageOutputStream(bytes)) {
                        writer.setOutput(out);
                        writer.write(null, new IIOImage(target, null, null), params);
                    }
                    target.flush();
                    return bytes.toByteArray();
                } finally {
                    writer.dispose();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
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
