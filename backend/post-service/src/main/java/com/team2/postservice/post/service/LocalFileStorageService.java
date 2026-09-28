package com.team2.postservice.post.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
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
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "게시글 이미지는 10MB 이하여야 합니다.");
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
        try (MemoryCacheImageInputStream in = new MemoryCacheImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                // 매우 큰 픽셀 수의 디코딩은 240MB 힙 컨테이너에서 메모리 부담이 크므로 방어적으로 제한한다.
                if (width < 1 || height < 1 || (long) width * height > 40_000_000L) return null;
                // 휴대폰 카메라 사진은 픽셀 자체는 눕혀 저장하고 EXIF Orientation 태그로 "이렇게
                // 돌려서 보여줘라"고만 표시하는 경우가 많다. 재인코딩하면서 이 태그가 사라지므로,
                // 픽셀에 직접 회전을 반영해두지 않으면 저장 후 사진이 돌아간 채로 보인다.
                BufferedImage original = applyExifOrientation(reader.read(0), readExifOrientation(reader));
                int origWidth = original.getWidth(), origHeight = original.getHeight();
                double ratio = Math.min(1.0, (double) MAX_DIMENSION / Math.max(origWidth, origHeight));
                int targetW = Math.max(1, (int) Math.round(origWidth * ratio));
                int targetH = Math.max(1, (int) Math.round(origHeight * ratio));
                BufferedImage target = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = target.createGraphics();
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
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
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

    // JPEG의 APP1(Exif) 마커 안에 있는 TIFF 구조를 뒤져 Orientation 태그(0x0112)를 찾는다.
    // 태그가 없거나 파싱에 실패하면 회전 안 함(1)으로 취급한다.
    private int readExifOrientation(ImageReader reader) {
        try {
            IIOMetadata metadata = reader.getImageMetadata(0);
            if (metadata == null) return 1;
            Element root = (Element) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
            NodeList markerSequence = root.getElementsByTagName("markerSequence");
            if (markerSequence.getLength() == 0) return 1;
            NodeList unknowns = ((Element) markerSequence.item(0)).getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                Element unknown = (Element) unknowns.item(i);
                if (!"225".equals(unknown.getAttribute("MarkerTag"))) continue; // APP1 = 0xE1 = 225
                Object data = ((IIOMetadataNode) unknown).getUserObject();
                if (data instanceof byte[] bytes) {
                    Integer orientation = parseExifOrientation(bytes);
                    if (orientation != null) return orientation;
                }
            }
        } catch (IllegalArgumentException | IOException e) {
            // 메타데이터 형식이 없거나 못 읽으면 회전 정보 없이 진행
        }
        return 1;
    }

    private Integer parseExifOrientation(byte[] app1) {
        if (app1 == null || app1.length < 14) return null;
        if (!(app1[0] == 'E' && app1[1] == 'x' && app1[2] == 'i' && app1[3] == 'f')) return null;
        int tiffStart = 6; // "Exif\0\0" 다음부터 TIFF 헤더
        if (tiffStart + 8 > app1.length) return null;
        boolean bigEndian;
        if (app1[tiffStart] == 'M' && app1[tiffStart + 1] == 'M') bigEndian = true;
        else if (app1[tiffStart] == 'I' && app1[tiffStart + 1] == 'I') bigEndian = false;
        else return null;

        int ifdOffset = readInt32(app1, tiffStart + 4, bigEndian);
        int ifdStart = tiffStart + ifdOffset;
        if (ifdStart < 0 || ifdStart + 2 > app1.length) return null;
        int numEntries = readInt16(app1, ifdStart, bigEndian);
        for (int i = 0; i < numEntries; i++) {
            int entryOffset = ifdStart + 2 + i * 12;
            if (entryOffset + 12 > app1.length) break;
            int tag = readInt16(app1, entryOffset, bigEndian);
            if (tag == 0x0112) {
                return readInt16(app1, entryOffset + 8, bigEndian);
            }
        }
        return null;
    }

    private int readInt16(byte[] b, int offset, boolean bigEndian) {
        int b0 = b[offset] & 0xFF, b1 = b[offset + 1] & 0xFF;
        return bigEndian ? (b0 << 8) | b1 : (b1 << 8) | b0;
    }

    private int readInt32(byte[] b, int offset, boolean bigEndian) {
        int b0 = b[offset] & 0xFF, b1 = b[offset + 1] & 0xFF, b2 = b[offset + 2] & 0xFF, b3 = b[offset + 3] & 0xFF;
        return bigEndian ? (b0 << 24) | (b1 << 16) | (b2 << 8) | b3 : (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    // EXIF Orientation 값(1~8)대로 실제 픽셀을 돌려서, 태그 없이 저장돼도 그대로 바르게 보이게 한다.
    // 7(가로 반전 + 90도 회전)은 단일 AffineTransform으로 조합하면 부호 실수가 나기 쉬워서,
    // 이미 검증된 5(전치)와 3(180도 회전)을 순서대로 두 번 적용하는 방식으로 만든다.
    private BufferedImage applyExifOrientation(BufferedImage image, int orientation) {
        if (orientation <= 1 || orientation > 8) return image;
        if (orientation == 7) {
            return applyExifOrientation(applyExifOrientation(image, 5), 3);
        }
        int width = image.getWidth(), height = image.getHeight();
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> { t.scale(-1.0, 1.0); t.translate(-width, 0); }
            case 3 -> { t.translate(width, height); t.rotate(Math.PI); }
            case 4 -> { t.scale(1.0, -1.0); t.translate(0, -height); }
            case 5 -> { t.rotate(0.5 * Math.PI); t.scale(1.0, -1.0); }
            case 6 -> { t.translate(height, 0); t.rotate(0.5 * Math.PI); }
            case 8 -> { t.translate(0, width); t.rotate(-0.5 * Math.PI); }
            default -> { return image; }
        }
        boolean swapDims = orientation == 5 || orientation == 6 || orientation == 8;
        BufferedImage rotated = new BufferedImage(
                swapDims ? height : width, swapDims ? width : height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rotated.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(image, t, null);
        } finally {
            g2d.dispose();
        }
        image.flush();
        return rotated;
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
