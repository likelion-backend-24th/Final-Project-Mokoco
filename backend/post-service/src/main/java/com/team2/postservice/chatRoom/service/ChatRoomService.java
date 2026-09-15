package com.team2.postservice.chatRoom.service;

import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
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
    private final com.team2.postservice.proposal.repository.ProposalRepository proposals;

    public java.util.List<com.team2.postservice.chatRoom.dto.ChatRoomListItem> getMyRooms(
            String authorization, int page, int size) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        if (page < 0 || size < 1 || size > 50)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 50");
        UserClientResponse user;
        try {
            user = userClient.verifyToken(authorization.substring(7));
        } catch (feign.FeignException failure) {
            if (failure.status() == 401)
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "User authentication service unavailable");
        }
        return chatRoomRepository.findMyRooms(user.id(), org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Transactional
    public ChatRoomResponse createChatRoom(Long fixDealId, String userEmail) {
        var user = userClient.getUserByEmail(userEmail);
        var deal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));
        return createForProposal(deal.getProposalId(), user.id());
    }

    @Transactional
    public ChatRoomResponse createForProposal(Long proposalId, Long userId) {
        // The proposal row serializes first creation and adoption across server instances.
        var proposal = proposals.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        var requester = userClient.getUserByEmail(proposal.getPost().getAuthorEmail());
        var repairer = userClient.getUserByEmail(proposal.getRepairerEmail());
        if (!userId.equals(requester.id()) && !userId.equals(repairer.id()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        if (requester.id().equals(repairer.id()))
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        var existing = findProposalRoom(proposalId);
        if (existing.isPresent()) return ChatRoomResponse.from(existing.get());
        var deal = fixDealRepository.findByProposalId(proposalId).orElse(null);
        var room = ChatRoom.builder().proposalId(proposalId).postId(proposal.getPost().getId())
                .requesterId(requester.id()).repairerId(repairer.id()).build();
        if (deal != null) room.attachDeal(deal);
        return ChatRoomResponse.from(chatRoomRepository.saveAndFlush(room));
    }

    public ChatRoomResponse getForProposal(Long proposalId, Long userId) {
        var proposal = proposals.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        var requester = userClient.getUserByEmail(proposal.getPost().getAuthorEmail());
        var repairer = userClient.getUserByEmail(proposal.getRepairerEmail());
        if (!userId.equals(requester.id()) && !userId.equals(repairer.id()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        return ChatRoomResponse.from(findProposalRoom(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)));
    }

    private java.util.Optional<ChatRoom> findProposalRoom(Long proposalId) {
        return chatRoomRepository.findByProposalId(proposalId).or(() ->
                fixDealRepository.findByProposalId(proposalId).flatMap(deal -> chatRoomRepository.findByFixDealId(deal.getId())));
    }

    public ChatRoomResponse detail(Long roomId, Long userId) {
        var room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.hasParticipant(userId)) throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        return ChatRoomResponse.from(room);
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
