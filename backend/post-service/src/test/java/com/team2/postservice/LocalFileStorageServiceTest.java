package com.team2.postservice;

import com.team2.postservice.post.service.FileStorageService;
import com.team2.postservice.post.service.LocalFileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageServiceTest {
    private final LocalFileStorageService storage = new LocalFileStorageService();
    private String storedFileName;

    @AfterEach
    void cleanup() throws Exception {
        if (storedFileName != null) Files.deleteIfExists(Paths.get("uploads").resolve(storedFileName));
    }

    @Test
    void resizesOversizedUploadAndConvertsToJpeg() throws Exception {
        BufferedImage big = new BufferedImage(3000, 2000, BufferedImage.TYPE_INT_RGB);
        var g = big.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, big.getWidth(), big.getHeight());
        g.dispose();
        var pngBytes = new ByteArrayOutputStream();
        ImageIO.write(big, "png", pngBytes);
        var upload = new MockMultipartFile("images", "big.png", "image/png", pngBytes.toByteArray());

        FileStorageService.StoredFile stored = storage.store(upload);
        storedFileName = stored.storedFileName();

        // 원본이 어떤 형식이든 항상 JPEG로 재인코딩해서 저장한다(정적 리소스 핸들러가 확장자로
        // Content-Type을 판단하므로 .jpg여야 image/jpeg로 서빙된다).
        assertThat(stored.storedFileName()).endsWith(".jpg");
        Path saved = Paths.get("uploads").resolve(stored.storedFileName());
        assertThat(Files.exists(saved)).isTrue();

        // 긴 변 1600px를 넘는 원본은 그 안으로 줄어든다.
        BufferedImage result = ImageIO.read(saved.toFile());
        assertThat(Math.max(result.getWidth(), result.getHeight())).isLessThanOrEqualTo(1600);
        assertThat(result.getWidth()).isLessThan(big.getWidth());
        assertThat(result.getHeight()).isLessThan(big.getHeight());
    }
}
