package com.team2.chatservice.chatRoom.service;

import com.team2.chatservice.chatRoom.dto.ChatRoomResponse;
import com.team2.chatservice.chatRoom.entity.ChatRoom;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.client.PostClient;
import com.team2.chatservice.client.UserClient;
import com.team2.chatservice.client.dto.FixDealResponse;
import com.team2.chatservice.client.dto.ProposalResponse;
import com.team2.chatservice.client.dto.UserClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final PostClient postClient;
    private final UserClient userClient;

    public java.util.List<com.team2.chatservice.chatRoom.dto.ChatRoomListItem> getMyRooms(
            String authorization, int page, int size) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        if (page < 0 || size < 1 || size > 50)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 50");
        UserClientResponse user;
        try {
            user = userClient.verifyToken(authorization.substring(7));
        } catch (FeignException failure) {
            if (failure.status() == 401)
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "User authentication service unavailable");
        }
        return chatRoomRepository.findMyRooms(user.id(), org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Transactional
    public ChatRoomResponse createChatRoom(Long fixDealId, String userEmail) {
        UserClientResponse user = userClient.getUserByEmail(userEmail);
        FixDealResponse fixDeal = requireFixDeal(fixDealId);

        if (!fixDeal.requesterId().equals(user.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_CREATE);
        }
        if (!"MATCHED".equals(fixDeal.status())) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        }
        if (chatRoomRepository.existsByFixDealId(fixDealId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .fixDealId(fixDeal.id())
                .proposalId(fixDeal.proposalId())
                .requesterId(fixDeal.requesterId())
                .repairerId(fixDeal.repairerId())
                .postId(fixDeal.postId())
                .build();

        return ChatRoomResponse.from(chatRoomRepository.save(chatRoom), fixDeal.status());
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoom(Long fixDealId, String userEmail) {
        FixDealResponse fixDeal = requireFixDeal(fixDealId);
        UserClientResponse user = userClient.getUserByEmail(userEmail);

        boolean participant = fixDeal.requesterId().equals(user.id()) || fixDeal.repairerId().equals(user.id());
        if (!participant) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }

        ChatRoom chatRoom = chatRoomRepository.findByFixDealId(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return ChatRoomResponse.from(chatRoom, fixDeal.status());
    }

    // 제안이 채택되기 전이라도 요청자/수리공 둘 중 누구나 채팅방을 열 수 있다(둘 다 방 생성 가능).
    // proposal_id에 DB unique 제약이 있어, 동시에 두 요청이 방을 만들려 하면 하나는 제약 위반으로
    // 실패한다 — 그 경우 예외를 삼키고 방금 다른 요청이 만든 방을 그대로 조회해서 반환한다.
    @Transactional
    public ChatRoomResponse createForProposal(Long proposalId, Long userId) {
        ProposalResponse proposal = requireProposal(proposalId);

        UserClientResponse requester = userClient.getUserByEmail(proposal.requesterEmail());
        UserClientResponse repairer = userClient.getUserByEmail(proposal.repairerEmail());
        if (!userId.equals(requester.id()) && !userId.equals(repairer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }
        if (requester.id().equals(repairer.id())) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        }

        var existing = chatRoomRepository.findByProposalId(proposalId);
        if (existing.isPresent()) return toResponse(existing.get());

        ChatRoom chatRoom = ChatRoom.builder()
                .proposalId(proposal.id())
                .postId(proposal.postId())
                .requesterId(requester.id())
                .repairerId(repairer.id())
                .build();

        ChatRoom saved;
        try {
            saved = chatRoomRepository.saveAndFlush(chatRoom);
        } catch (DataIntegrityViolationException raceLoser) {
            saved = chatRoomRepository.findByProposalId(proposalId)
                    .orElseThrow(() -> raceLoser);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getForProposal(Long proposalId, Long userId) {
        ProposalResponse proposal = requireProposal(proposalId);

        UserClientResponse requester = userClient.getUserByEmail(proposal.requesterEmail());
        UserClientResponse repairer = userClient.getUserByEmail(proposal.repairerEmail());
        if (!userId.equals(requester.id()) && !userId.equals(repairer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }

        ChatRoom room = chatRoomRepository.findByProposalId(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse detail(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.hasParticipant(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }
        return toResponse(room);
    }

    private ChatRoomResponse toResponse(ChatRoom room) {
        String dealStatus = room.getFixDealId() == null ? null : dealStatusOrNull(room.getFixDealId());
        return ChatRoomResponse.from(room, dealStatus);
    }

    private String dealStatusOrNull(Long fixDealId) {
        try {
            return postClient.getFixDeal(fixDealId).status();
        } catch (FeignException e) {
            return null;
        }
    }

    private FixDealResponse requireFixDeal(Long fixDealId) {
        try {
            return postClient.getFixDeal(fixDealId);
        } catch (FeignException.NotFound e) {
            throw new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND);
        }
    }

    private ProposalResponse requireProposal(Long proposalId) {
        try {
            return postClient.getProposal(proposalId);
        } catch (FeignException.NotFound e) {
            throw new CustomException(ErrorCode.PROPOSAL_NOT_FOUND);
        }
    }
}
