package com.team2.postservice.ai;

import com.team2.postservice.common.exception.AiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AiImagesTest {
    final AiImages images = new AiImages();
    static byte[] png(int width, int height) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB),"png",bytes); return bytes.toByteArray();
    }
    @Test void convertsAndDownsizesActualPixels() throws Exception {
        var part = images.parts(List.of(new MockMultipartFile("images","photo.png","image/png",png(2000,1000)))).getFirst();
        var data = (Map<?,?>) part.get("inlineData");
        assertThat(data.get("mimeType")).isEqualTo("image/jpeg");
        var decoded = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode((String)data.get("data"))));
        assertThat(decoded.getWidth()).isEqualTo(1600); assertThat(decoded.getHeight()).isEqualTo(800);
    }
    @Test void rejectsForgedMimeAndCorruptImage() throws Exception {
        var forged = new MockMultipartFile("images","file.jpg","image/jpeg",png(10,10));
        assertThatThrownBy(() -> images.parts(List.of(forged))).isInstanceOf(AiException.class);
        assertThatThrownBy(() -> images.parts(List.of(new MockMultipartFile("images","file.png","image/png","bad".getBytes())))).isInstanceOf(AiException.class);
    }
    @Test void rejectsEmptySixImagesAndOversizedFile() throws Exception {
        var file = new MockMultipartFile("images","file.png","image/png",png(10,10));
        assertThatThrownBy(() -> images.parts(List.of())).isInstanceOf(AiException.class);
        assertThatThrownBy(() -> images.parts(Collections.nCopies(6,file))).isInstanceOf(AiException.class);
        assertThatThrownBy(() -> images.parts(List.of(new MockMultipartFile("images",new byte[5*1024*1024+1])))).isInstanceOf(AiException.class);
    }
    @Test void webpDecoderIsInstalled() { assertThat(ImageIO.getImageReadersByFormatName("webp").hasNext()).isTrue(); }
}
