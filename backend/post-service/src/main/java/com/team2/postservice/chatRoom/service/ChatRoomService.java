package com.team2.postservice.chatRoom.service;

import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final FixDealRepository fixDealRepository;
    private final UserClient userClient;

    @Transactional
    public ChatRoomResponse createChatRoom(Long fixDealId, String userEmail){

        UserClientResponse user = userClient.getUserByEmail(userEmail);

        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        if(!fixDeal.getRequesterId().equals(user.id())){
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_CREATE);
        }

        if(fixDeal.getStatus() != FixDealStatus.MATCHED){
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        }

        if (chatRoomRepository.existsByFixDealId(fixDealId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .fixDeal(fixDeal)
                .build();

        return ChatRoomResponse.from(chatRoomRepository.save(chatRoom));

    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoom(Long fixDealId, String userEmail) {

        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        UserClientResponse user = userClient.getUserByEmail(userEmail);

        boolean participant =
                fixDeal.getRequesterId().equals(user.id())
                        || fixDeal.getRepairerId().equals(user.id());

        if (!participant) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }

        ChatRoom chatRoom = chatRoomRepository.findByFixDealId(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return ChatRoomResponse.from(chatRoom);
    }
}
