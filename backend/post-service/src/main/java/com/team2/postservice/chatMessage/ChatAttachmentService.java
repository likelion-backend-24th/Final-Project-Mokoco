package com.team2.postservice.chatMessage;

import com.team2.postservice.chatMessage.dto.ChatMessageResponse;
import com.team2.postservice.chatMessage.entity.*;
import com.team2.postservice.chatMessage.repository.ChatMessageRepository;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatAttachmentService {
    private final ChatService chat;
    private final ChatMessageRepository messages;
    private final Path root;
    private static final Set<String> IMAGES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> VIDEOS = Set.of("video/mp4", "video/webm");

    public ChatAttachmentService(ChatService chat, ChatMessageRepository messages,
            @Value("${chat.attachment-dir:./private-chat-files}") String directory) {
        this.chat = chat; this.messages = messages;
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public ChatMessageResponse upload(Long roomId, Long userId, MultipartFile file) throws IOException {
        var room = chat.authorize(roomId, userId);
        if (file.isEmpty() || file.getSize() > 50L * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "파일은 1바이트 이상 50MB 이하여야 합니다.");
        String mime;
        try (var input = file.getInputStream()) { mime = new Tika().detect(input); }
        // Some MP4 files are detected as QuickTime; verify ISO container brands before normalizing.
        if (("application/mp4".equals(mime) || "video/quicktime".equals(mime)) && hasMp4Brand(file)) mime = "video/mp4";
        boolean image = IMAGES.contains(mime);
        if (!image && !VIDEOS.contains(mime))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "JPG, PNG, GIF, WebP, MP4, WebM만 지원합니다.");
        if (image && file.getSize() > 10L * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "이미지는 10MB 이하여야 합니다.");
        Files.createDirectories(root);
        String key = UUID.randomUUID().toString();
        Path target = root.resolve(key);
        // Filesystem writes are compensated if the database transaction rolls back.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { Files.deleteIfExists(target); } catch (IOException ignored) { /* cleanup can be retried by maintenance */ }
                }
            }
        });
        try (var input = file.getInputStream()) { Files.copy(input, target); }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("attachment")
                .replaceAll("[\\\\/\\p{Cntrl}]", "_");
        if (name.length() > 180) name = name.substring(0, 180);
        var message = messages.save(ChatMessage.builder().chatRoom(room).senderId(userId)
                .content(image ? "사진" : "동영상").messageType(image ? MessageType.IMAGE : MessageType.VIDEO)
                .createdAt(LocalDateTime.now()).attachmentKey(key).attachmentName(name)
                .attachmentMime(mime).attachmentSize(file.getSize()).build());
        return ChatMessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long roomId, Long messageId, Long userId) {
        chat.authorize(roomId, userId);
        var message = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!message.getChatRoom().getId().equals(roomId) || message.getAttachmentKey() == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Path path = root.resolve(message.getAttachmentKey()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(message.getAttachmentMime()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", ContentDisposition.inline().filename(message.getAttachmentName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(path));
    }

    private boolean hasMp4Brand(MultipartFile file) throws IOException {
        byte[] header;
        try (var input = file.getInputStream()) { header = input.readNBytes(4096); }
        if (header.length < 16) return false;
        var charset = java.nio.charset.StandardCharsets.US_ASCII;
        if (!new String(header, 4, 4, charset).equals("ftyp")) return false;
        long boxSize = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(header).getInt());
        if (boxSize < 16 || boxSize > header.length || boxSize % 4 != 0) return false;
        var brands = Set.of("isom", "iso2", "iso3", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "dash", "M4V ");
        for (int offset = 8; offset + 4 <= boxSize; offset += 4) {
            if (offset != 12 && brands.contains(new String(header, offset, 4, charset))) return true;
        }
        return false;
    }
}
