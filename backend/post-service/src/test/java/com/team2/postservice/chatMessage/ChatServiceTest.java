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
    @Test void resolvesCounterpartOnlyForParticipants() {
        room();
        assertThat(service.counterpartId(1L, 2L)).isEqualTo(3L);
        assertThat(service.counterpartId(1L, 3L)).isEqualTo(2L);
        assertThatThrownBy(() -> service.counterpartId(1L, 9L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
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
    @Test void onlySenderCanDeleteAndDeletionHidesAttachment() {
        room();
        var message = ChatMessage.builder().id(7L).chatRoom(rooms.findById(1L).orElseThrow())
                .senderId(2L).content("photo").attachmentKey("private-file").attachmentName("name.png").build();
        when(messages.findById(7L)).thenReturn(Optional.of(message));
        assertThatThrownBy(() -> service.delete(1L, 7L, 3L)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(message.getDeletedAt()).isNull();
        var result = service.delete(1L, 7L, 2L);
        assertThat(result.deleted()).isTrue();
        assertThat(result.content()).isEqualTo("삭제된 메시지입니다.");
        assertThat(result.attachmentUrl()).isNull();
        assertThat(result.attachmentName()).isNull();
        var deletedAt = message.getDeletedAt();
        service.delete(1L, 7L, 2L);
        assertThat(message.getDeletedAt()).isEqualTo(deletedAt);
    }
    @Test void cannotDeleteMessageThroughDifferentRoom() {
        room();
        when(messages.findById(7L)).thenReturn(Optional.of(ChatMessage.builder().id(7L)
                .chatRoom(ChatRoom.builder().id(99L).build()).senderId(2L).build()));
        assertThatThrownBy(() -> service.delete(1L, 7L, 2L)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
