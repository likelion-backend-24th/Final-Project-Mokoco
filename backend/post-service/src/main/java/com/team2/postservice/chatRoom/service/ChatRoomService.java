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
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final FixDealRepository fixDealRepository;
    private final ProposalRepository proposalRepository;
    private final UserClient userClient;

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

    // 제안이 채택되기 전이라도 요청자/수리공 둘 중 누구나 채팅방을 열 수 있다(둘 다 방 생성 가능).
    // proposalId는 Proposal 행을 잠가서(lockById) 동시에 방을 만들려는 요청/채택 처리와의 경합을 막는다.
    @Transactional
    public ChatRoomResponse createForProposal(Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        UserClientResponse requester = userClient.getUserByEmail(proposal.getPost().getAuthorEmail());
        UserClientResponse repairer = userClient.getUserByEmail(proposal.getRepairerEmail());
        if (!userId.equals(requester.id()) && !userId.equals(repairer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }
        if (requester.id().equals(repairer.id())) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        }

        var existing = findExistingRoom(proposal);
        if (existing.isPresent()) return ChatRoomResponse.from(existing.get());

        // 이미 채택돼 FixDeal이 존재하는데(예전 방식으로 채택은 됐지만 방이 없던 경우) 방을 새로 만드는 것이면,
        // 바로 그 거래를 이어붙여서 만든다.
        FixDeal deal = fixDealRepository.findByProposalId(proposalId).orElse(null);
        ChatRoom chatRoom = ChatRoom.builder()
                .proposalId(proposal.getId())
                .postId(proposal.getPost().getId())
                .requesterId(requester.id())
                .repairerId(repairer.id())
                .build();
        if (deal != null) chatRoom.attachDeal(deal);

        return ChatRoomResponse.from(chatRoomRepository.saveAndFlush(chatRoom));
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getForProposal(Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        UserClientResponse requester = userClient.getUserByEmail(proposal.getPost().getAuthorEmail());
        UserClientResponse repairer = userClient.getUserByEmail(proposal.getRepairerEmail());
        if (!userId.equals(requester.id()) && !userId.equals(repairer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }

        return findExistingRoom(proposal)
                .map(ChatRoomResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse detail(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.hasParticipant(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        }
        return ChatRoomResponse.from(room);
    }

    // 제안 기준으로 먼저 찾고, 없으면 (채택 이후 예전 방식대로 fixDealId 경유로 만들어진) 레거시 방도 찾아본다.
    private java.util.Optional<ChatRoom> findExistingRoom(Proposal proposal) {
        java.util.Optional<ChatRoom> byProposal = chatRoomRepository.findByProposalId(proposal.getId());
        if (byProposal.isPresent()) return byProposal;
        return fixDealRepository.findByProposalId(proposal.getId())
                .flatMap(deal -> chatRoomRepository.findByFixDealId(deal.getId()));
    }
}
