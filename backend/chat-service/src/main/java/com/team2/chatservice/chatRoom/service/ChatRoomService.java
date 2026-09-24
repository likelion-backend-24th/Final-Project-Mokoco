package com.team2.chatservice.chatRoom.service;

import com.team2.chatservice.chatRoom.dto.ChatRoomListItem;
import com.team2.chatservice.chatRoom.dto.ChatRoomListResponse;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserClient userClient;

    public List<ChatRoomListResponse> getMyRooms(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 50");
        List<ChatRoomListItem> rooms = chatRoomRepository.findMyRooms(userId, PageRequest.of(page, size));
        return rooms.stream().map(this::toResponse).toList();
    }

    // 목록 화면은 글 제목보다 대화 상대 닉네임이 더 유용해서 user-service에서 채워 넣는다.
    // 한 명 조회가 실패해도 나머지 방 목록은 그대로 보여줘야 하므로 실패는 무시하고 null로 둔다.
    private ChatRoomListResponse toResponse(ChatRoomListItem item) {
        String nickname = null;
        try {
            nickname = userClient.getNickname(item.getCounterpartId()).nickname();
        } catch (Exception e) {
            log.warn("채팅방 목록 닉네임 조회 실패: counterpartId={}", item.getCounterpartId(), e);
        }
        return new ChatRoomListResponse(
                item.getChatRoomId(), item.getFixDealId(), item.getPostId(), item.getPostTitle(),
                item.getCounterpartId(), nickname,
                item.getLastMessage(), item.getLastMessageAt(), item.getCreatedAt()
        );
    }
}
