package com.team2.chatservice.chatRoom;

import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.chatRoom.service.ChatRoomService;
import com.team2.chatservice.client.UserClient;
import com.team2.chatservice.client.dto.UserClientResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ChatRoomListServiceTest {
    final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    final UserClient users = mock(UserClient.class);
    final ChatRoomService service = new ChatRoomService(rooms, users, mock(com.team2.chatservice.client.PostClient.class), mock(org.springframework.transaction.PlatformTransactionManager.class));

    @Test void usesVerifiedUserId() {
        when(users.verifyToken("token")).thenReturn(new UserClientResponse(7L, "user@example.com", "user", "region"));
        service.getMyRooms("Bearer token", 0, 5);
        verify(rooms).findMyRooms(7L, PageRequest.of(0, 5));
    }
    @Test void rejectsMissingTokenAndInvalidPaginationBeforeQuerying() {
        assertThatThrownBy(() -> service.getMyRooms(null, 0, 5)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.getMyRooms("Bearer token", -1, 5)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.getMyRooms("Bearer token", 0, 51)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(rooms, users);
    }
}
