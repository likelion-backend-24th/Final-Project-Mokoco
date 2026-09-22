package com.team2.postservice.ai;

import com.team2.postservice.common.exception.AiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

@Component
public class AiImages {
    public List<Map<String, Object>> parts(List<MultipartFile> files) {

        if (files == null || files.isEmpty() || files.size() > 5) {
            throw AiException.input("분석할 사진을 1~5장 선택해주세요.");
        }

        long total = files.stream().mapToLong(MultipartFile::getSize).sum();

        if (total > 15 * 1024 * 1024 || files.stream().anyMatch(f -> f.getSize() > 5 * 1024 * 1024)){
            throw new AiException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", "AI 분석 사진은 장당 5MB, 합계 15MB 이하여야 합니다.");
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (MultipartFile file : files) {
            try (MemoryCacheImageInputStream stream = new MemoryCacheImageInputStream(file.getInputStream())) {

                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

                if (!readers.hasNext()) {
                    throw unsupported();
                }

                ImageReader reader = readers.next();

                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);

                    if (!Set.of("jpeg", "jpg", "png", "webp").contains(format)) {
                        throw unsupported();
                    }

                    String mime = "image/" + (format.equals("jpg") ? "jpeg" : format);

                    if (!mime.equalsIgnoreCase(file.getContentType())) {
                        throw unsupported();
                    }

                    reader.setInput(stream, true, true);

                    int width = reader.getWidth(0), height = reader.getHeight(0);

                    if (width < 1 || height < 1 || (long) width * height > 25_000_000L){
                        throw new AiException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_PIXELS", "사진 해상도를 2,500만 화소 이하로 줄여주세요.");
                    }

                    BufferedImage original = reader.read(0);

                    double ratio = Math.min(1.0, 1600.0 / Math.max(width, height));

                    BufferedImage resized = new BufferedImage(Math.max(1, (int) (width * ratio)), Math.max(1, (int) (height * ratio)), BufferedImage.TYPE_INT_RGB);

                    Graphics2D g = resized.createGraphics();

                    try {
                        g.setColor(java.awt.Color.WHITE); g.fillRect(0, 0, resized.getWidth(), resized.getHeight());
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g.drawImage(original, 0, 0, resized.getWidth(), resized.getHeight(), null);

                    } finally {
                        g.dispose(); original.flush();
                    }

                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(resized, "jpeg", bytes); resized.flush();

                    result.add(Map.of("inlineData", Map.of("mimeType", "image/jpeg", "data", Base64.getEncoder().encodeToString(bytes.toByteArray()))));

                } finally {
                    reader.dispose();
                }
            } catch (AiException e) {
                throw e;
            } catch (IOException | RuntimeException e) {
                throw unsupported();
            }
        }
        return result;
    }
    private AiException unsupported() {
        return new AiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_IMAGE", "올바른 JPEG, PNG 또는 WebP 사진을 선택해주세요.");
    }
}
