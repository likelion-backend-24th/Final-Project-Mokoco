package com.team2.chatservice.chatMessage;

import com.team2.chatservice.chatMessage.dto.ChatMessageResponse;
import com.team2.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms/{roomId}/attachments")
public class ChatAttachmentController {
    private final ChatAttachmentService attachments;
    private final SimpMessagingTemplate broker;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatMessageResponse upload(@PathVariable Long roomId,
            @AuthenticationPrincipal LoginUser user,
            @RequestPart("file") MultipartFile file) throws IOException {
        var saved = attachments.upload(roomId, user.id(), file);
        broker.convertAndSend("/topic/chat/" + roomId, saved);
        return saved;
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<Resource> download(@PathVariable Long roomId, @PathVariable Long messageId,
            @AuthenticationPrincipal LoginUser user) {
        return attachments.download(roomId, messageId, user.id());
    }
}
