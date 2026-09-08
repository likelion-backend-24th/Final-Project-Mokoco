package com.team2.postservice.chatMessage;

import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.chatMessage.entity.ChatMessage;
import com.team2.postservice.chatMessage.repository.ChatMessageRepository;
import com.team2.postservice.fixDeal.entity.FixDeal;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ChatServiceTest {
    final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    final ChatService service = new ChatService(rooms, messages);

    void room() {
        when(rooms.findById(1L)).thenReturn(Optional.of(ChatRoom.builder().id(1L)
                .fixDeal(FixDeal.builder().requesterId(2L).repairerId(3L).build()).build()));
    }
    @Test void rejectsOutsidersForReadAndWrite() {
        room();
        assertThatThrownBy(() -> service.history(1L, 9L, null)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> service.send(1L, 9L, "hello")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(messages);
    }
    @Test void savesWithVerifiedSenderAndTimestamp() {
        room();
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.send(1L, 3L, " hello ");
        assertThat(result.senderId()).isEqualTo(3L);
        assertThat(result.content()).isEqualTo("hello");
        assertThat(result.createdAt()).isNotNull();
        verify(messages).save(any(ChatMessage.class));
    }
    @Test void rejectsBlankAndOversizedMessages() {
        room();
        assertThatThrownBy(() -> service.send(1L, 2L, " ")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> service.send(1L, 2L, "x".repeat(2001))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(messages);
    }
}
