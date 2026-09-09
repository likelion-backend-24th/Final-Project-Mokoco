package com.team2.postservice.chatMessage;

import com.team2.postservice.chatMessage.entity.*;
import com.team2.postservice.chatMessage.repository.ChatMessageRepository;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ChatAttachmentTest {
    @TempDir Path directory;
    final ChatService chat = mock(ChatService.class);
    final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    ChatAttachmentService service;
    @BeforeEach void setup() {
        service = new ChatAttachmentService(chat, messages, directory.toString());
        when(chat.authorize(1L, 2L)).thenReturn(ChatRoom.builder().id(1L).build());
        when(messages.save(any())).thenAnswer(call -> call.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();
    }
    @AfterEach void cleanup() { TransactionSynchronizationManager.clearSynchronization(); }
    @Test void detectsActualImageRatherThanDeclaredMimeAndCleansRollback() throws Exception {
        byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1cAAAAASUVORK5CYII=");
        var result = service.upload(1L, 2L, new MockMultipartFile("file", "../photo.png", "text/plain", png));
        assertThat(result.type()).isEqualTo(MessageType.IMAGE);
        assertThat(result.attachmentMime()).isEqualTo("image/png");
        assertThat(result.attachmentName()).doesNotContain("/");
        try (var files = Files.list(directory)) { assertThat(files.count()).isEqualTo(1); }
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        try (var files = Files.list(directory)) { assertThat(files.count()).isZero(); }
    }
    @Test void rejectsDisguisedHtml() {
        assertThatThrownBy(() -> service.upload(1L, 2L,
                new MockMultipartFile("file", "image.jpg", "image/jpeg", "<html><script>alert(1)</script></html>".getBytes())))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(messages);
    }
    @Test void acceptsMp4ContainerDetectedFromFileBytes() throws Exception {
        // ISO Base Media container with the common isom/mp42 compatible brands.
        byte[] mp4 = java.nio.ByteBuffer.allocate(28).putInt(28)
                .put("ftypisom".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                .putInt(512).put("isomiso2mp42".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).array();
        assertThat(new org.apache.tika.Tika().detect(mp4)).isEqualTo("video/quicktime");
        var result = service.upload(1L, 2L, new MockMultipartFile("file", "a8.mp4", "video/mp4", mp4));
        assertThat(result.type()).isEqualTo(MessageType.VIDEO);
        assertThat(result.attachmentMime()).isEqualTo("video/mp4");
    }
    @Test void rejectsUnauthorizedUploadBeforeStorage() {
        doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(chat).authorize(1L, 9L);
        assertThatThrownBy(() -> service.upload(1L, 9L, new MockMultipartFile("file", new byte[]{1})))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(messages);
    }
    @Test void rejectsAttachmentFromAnotherRoom() {
        when(messages.findById(3L)).thenReturn(java.util.Optional.of(ChatMessage.builder()
                .chatRoom(ChatRoom.builder().id(99L).build()).attachmentKey("file").build()));
        assertThatThrownBy(() -> service.download(1L, 3L, 2L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void deletedAttachmentCannotBeDownloaded() {
        var message = ChatMessage.builder().chatRoom(ChatRoom.builder().id(1L).build())
                .attachmentKey("file").content("photo").build();
        message.delete();
        when(messages.findById(3L)).thenReturn(java.util.Optional.of(message));
        assertThatThrownBy(() -> service.download(1L, 3L, 2L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
