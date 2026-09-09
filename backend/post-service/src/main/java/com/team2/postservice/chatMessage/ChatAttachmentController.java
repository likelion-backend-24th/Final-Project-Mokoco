package com.team2.postservice.chatMessage;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.chatMessage.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms/{roomId}/attachments")
public class ChatAttachmentController {
    private final ChatAttachmentService attachments;
    private final UserClient users;
    private final SimpMessagingTemplate broker;

    private Long userId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.verifyToken(auth.substring(7)).id();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatMessageResponse upload(@PathVariable Long roomId,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestPart("file") MultipartFile file) throws IOException {
        var saved = attachments.upload(roomId, userId(auth), file);
        broker.convertAndSend("/topic/chat/" + roomId, saved);
        return saved;
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<Resource> download(@PathVariable Long roomId, @PathVariable Long messageId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return attachments.download(roomId, messageId, userId(auth));
    }
}
