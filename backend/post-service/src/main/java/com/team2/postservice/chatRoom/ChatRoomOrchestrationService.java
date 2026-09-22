package com.team2.postservice.chatRoom;

import com.team2.common.exception.CustomException;
import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 제안/거래 단위로 채팅방을 열고 조회하는 오케스트레이션. Proposal/FixDeal은 post-service가 로컬
// DB에 이미 갖고 있으므로, chat-service를 왕복 조회할 필요 없이 여기서 컨텍스트를 만들어 한 번만
// chat-service에 넘긴다(chat-service는 받은 컨텍스트로 방을 찾거나 만드는 저장소 역할만 함).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomOrchestrationService {
    private final ProposalRepository proposalRepository;
    private final FixDealRepository fixDealRepository;
    private final UserClient userClient;
    private final ChatRoomClient chatRoomClient;

    @Transactional
    public ChatRoomResponse createForProposal(Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        Participants participants = authorize(proposal, userId);
        ChatRoomClient.ChatRoomInfo room = chatRoomClient.ensureRoomForProposal(
                new ChatRoomClient.ProposalRoomContext(proposal.getId(), proposal.getPost().getId(),
                        participants.requesterId(), participants.repairerId()));
        return ChatRoomResponse.from(room, dealStatus(proposal));
    }

    public ChatRoomResponse getForProposal(Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        authorize(proposal, userId);
        return ChatRoomResponse.from(requireRoomByProposal(proposalId), dealStatus(proposal));
    }

    @Transactional
    public ChatRoomResponse createForFixDeal(Long fixDealId, Long userId) {
        FixDeal deal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));
        if (!deal.getRequesterId().equals(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_CREATE);
        if (deal.getStatus() != FixDealStatus.MATCHED)
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        ChatRoomClient.ChatRoomInfo room = chatRoomClient.ensureRoomForProposal(
                new ChatRoomClient.ProposalRoomContext(deal.getProposalId(), deal.getPostId(),
                        deal.getRequesterId(), deal.getRepairerId()));
        return ChatRoomResponse.from(room, deal.getStatus().name());
    }

    public ChatRoomResponse getForFixDeal(Long fixDealId, Long userId) {
        FixDeal deal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));
        if (!deal.getRequesterId().equals(userId) && !deal.getRepairerId().equals(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        return ChatRoomResponse.from(requireRoomByProposal(deal.getProposalId()), deal.getStatus().name());
    }

    public ChatRoomResponse detail(Long roomId, Long userId) {
        ChatRoomClient.ChatRoomInfo room;
        try {
            room = chatRoomClient.getRoom(roomId);
        } catch (FeignException.NotFound e) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        if (!userId.equals(room.requesterId()) && !userId.equals(room.repairerId()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        String dealStatus = room.fixDealId() == null ? null
                : fixDealRepository.findById(room.fixDealId()).map(deal -> deal.getStatus().name()).orElse(null);
        return ChatRoomResponse.from(room, dealStatus);
    }

    private ChatRoomClient.ChatRoomInfo requireRoomByProposal(Long proposalId) {
        try {
            return chatRoomClient.getRoomByProposal(proposalId);
        } catch (FeignException.NotFound e) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    private record Participants(Long requesterId, Long repairerId) {}

    private Participants authorize(Proposal proposal, Long userId) {
        UserClientResponse requester = userClient.getUserById(proposal.getPost().getAuthorId());
        UserClientResponse repairer = userClient.getUserById(proposal.getRepairerId());
        if (!userId.equals(requester.id()) && !userId.equals(repairer.id()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        if (requester.id().equals(repairer.id()))
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        return new Participants(requester.id(), repairer.id());
    }

    private String dealStatus(Proposal proposal) {
        if (!proposal.isAdopted()) return null;
        return fixDealRepository.findByProposalId(proposal.getId())
                .map(deal -> deal.getStatus().name()).orElse(null);
    }
}
