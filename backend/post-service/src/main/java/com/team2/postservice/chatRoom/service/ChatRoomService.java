package com.team2.postservice.chatRoom.service;

import com.team2.postservice.chatRoom.dto.ChatRoomListItem;
import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final FixDealRepository fixDealRepository;
    private final UserClient userClient;
    private final ProposalRepository proposals;

    public List<ChatRoomListItem> getMyRooms(
            String authorization, int page, int size) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (page < 0 || size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 50");
        UserClientResponse user;
        try {
            user = userClient.verifyToken(authorization.substring(7));
        } catch (feign.FeignException failure) {
            if (failure.status() == 401)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "User authentication service unavailable");
        }
        return chatRoomRepository.findMyRooms(user.id(), PageRequest.of(page, size));
    }

    @Transactional
    public ChatRoomResponse createChatRoom(Long fixDealId, String userEmail) {
        UserClientResponse user = userClient.getUserByEmail(userEmail);
        FixDeal deal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        return createForProposal(deal.getProposalId(), user.id());
    }

    @Transactional
    public ChatRoomResponse createForProposal(Long proposalId, Long userId) {
        // The proposal row serializes first creation and adoption across server instances.
        Proposal proposal = proposals.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        UserClientResponse requester = userClient.getUserByEmail(proposal.getPost().getAuthorEmail());

        UserClientResponse repairer = userClient.getUserByEmail(proposal.getRepairerEmail());

        if (!userId.equals(requester.id()) && !userId.equals(repairer.id()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);

        if (requester.id().equals(repairer.id()))
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);

        Optional<ChatRoom> existing = findProposalRoom(proposalId);

        if (existing.isPresent()) {
            return ChatRoomResponse.from(existing.get());
        }

        FixDeal deal = fixDealRepository.findByProposalId(proposalId).orElse(null);

        ChatRoom room = ChatRoom.builder().proposalId(proposalId).postId(proposal.getPost().getId())
                .requesterId(requester.id()).repairerId(repairer.id()).build();

        if (deal != null) room.attachDeal(deal);

        return ChatRoomResponse.from(chatRoomRepository.saveAndFlush(room));
    }

    public ChatRoomResponse getForProposal(Long proposalId, Long userId) {

        Proposal proposal = proposals.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        UserClientResponse requester = userClient.getUserByEmail(proposal.getPost().getAuthorEmail());

        UserClientResponse repairer = userClient.getUserByEmail(proposal.getRepairerEmail());

        if (!userId.equals(requester.id()) && !userId.equals(repairer.id()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);

        return ChatRoomResponse.from(findProposalRoom(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)));
    }

    private Optional<ChatRoom> findProposalRoom(Long proposalId) {
        return chatRoomRepository.findByProposalId(proposalId).or(() ->
                fixDealRepository.findByProposalId(proposalId).flatMap(deal -> chatRoomRepository.findByFixDealId(deal.getId())));
    }

    public ChatRoomResponse detail(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.hasParticipant(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }

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
