package com.team2.chatservice.chatRoom.service;

import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    public java.util.List<com.team2.chatservice.chatRoom.dto.ChatRoomListItem> getMyRooms(
            Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 50)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 50");
        return chatRoomRepository.findMyRooms(userId, org.springframework.data.domain.PageRequest.of(page, size));
    }
}
