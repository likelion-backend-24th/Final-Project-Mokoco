package com.team2.chatservice.chatRoom;

import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.chatRoom.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ChatRoomListServiceTest {
    final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    final ChatRoomService service = new ChatRoomService(rooms);

    // 토큰 검증 자체는 SecurityConfig의 TokenAuthenticationFilter가 담당하므로(userId를 컨트롤러가
    // 이미 확인된 값으로 넘겨받음), 서비스는 페이지네이션 검증만 스스로 한다.
    @Test void queriesRoomsForGivenUserId() {
        service.getMyRooms(7L, 0, 5);
        verify(rooms).findMyRooms(7L, PageRequest.of(0, 5));
    }
    @Test void rejectsInvalidPaginationBeforeQuerying() {
        assertThatThrownBy(() -> service.getMyRooms(7L, -1, 5)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> service.getMyRooms(7L, 0, 51)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(rooms);
    }
}
