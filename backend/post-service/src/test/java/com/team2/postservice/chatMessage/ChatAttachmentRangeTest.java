package com.team2.postservice.chatMessage;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatAttachmentRangeTest {
    @TempDir Path directory;
    @Test void returnsPartialVideoContentAfterAuthentication() throws Exception {
        Path file = Files.write(directory.resolve("video"), new byte[]{0,1,2,3,4,5,6,7});
        var service = mock(ChatAttachmentService.class);
        var users = mock(UserClient.class);
        when(users.verifyToken("token")).thenReturn(new UserClientResponse(2L, "u", "u", null));
        when(service.download(1L, 3L, 2L)).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4")).body(new FileSystemResource(file)));
        var mvc = MockMvcBuilders.standaloneSetup(new ChatAttachmentController(service, users, mock(SimpMessagingTemplate.class))).build();
        mvc.perform(get("/api/chat-rooms/1/attachments/3").header("Authorization", "Bearer token").header("Range", "bytes=2-4"))
                .andExpect(status().isPartialContent()).andExpect(header().string("Content-Range", "bytes 2-4/8"))
                .andExpect(content().bytes(new byte[]{2,3,4}));
        verify(service).download(1L, 3L, 2L);
        mvc.perform(get("/api/chat-rooms/1/attachments/3")).andExpect(status().isUnauthorized());
    }
}
