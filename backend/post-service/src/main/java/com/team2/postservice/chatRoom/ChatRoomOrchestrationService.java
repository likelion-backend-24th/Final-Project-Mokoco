package com.team2.postservice.chatRoom;

import com.team2.common.chat.ChatRoomInfo;
import com.team2.common.chat.ProposalChatResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import com.team2.postservice.client.ChatClient;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomOrchestrationService {
    private final ProposalRepository proposals;
    private final FixDealRepository deals;
    private final ChatClient chat;

    @Transactional
    public ChatRoomResponse createForProposal(Long proposalId, Long userId) {
        Proposal proposal = proposals.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        ProposalChatResponse context = authorize(context(proposal), userId);
        return ChatRoomResponse.from(chat.ensureRoom(context));
    }

    public ChatRoomResponse getForProposal(Long proposalId, Long userId) {
        Proposal proposal = proposals.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        return ChatRoomResponse.from(chat.syncProposal(authorize(context(proposal), userId)));
    }

    @Transactional
    public ChatRoomResponse createForFixDeal(Long fixDealId, Long userId) {
        FixDeal deal = deals.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));
        if (!deal.getRequesterId().equals(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_CREATE);
        Proposal proposal = proposals.lockById(deal.getProposalId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        return ChatRoomResponse.from(chat.ensureRoom(context(proposal)));
    }

    public ChatRoomResponse getForFixDeal(Long fixDealId, Long userId) {
        FixDeal deal = deals.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));
        if (!deal.getRequesterId().equals(userId) && !deal.getRepairerId().equals(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        Proposal proposal = proposals.findById(deal.getProposalId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        return ChatRoomResponse.from(chat.syncProposal(context(proposal)));
    }

    public ChatRoomResponse detail(Long roomId, Long userId) {
        ChatRoomInfo room = chat.getRoom(roomId);
        if (!room.hasParticipant(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        if (room.proposalId() != null) {
            Proposal proposal = proposals.findById(room.proposalId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
            room = chat.syncProposal(authorize(context(proposal), userId));
        }
        return ChatRoomResponse.from(room);
    }

    private ProposalChatResponse context(Proposal proposal) {
        FixDeal deal = proposal.isAdopted() ? deals.findByProposalId(proposal.getId()).orElse(null) : null;
        return new ProposalChatResponse(proposal.getId(), proposal.getPost().getId(), proposal.getPost().getTitle(),
                deal == null ? proposal.getPost().getAuthorId() : deal.getRequesterId(),
                deal == null ? proposal.getRepairerId() : deal.getRepairerId(), deal == null ? null : deal.getId());
    }

    private ProposalChatResponse authorize(ProposalChatResponse context, Long userId) {
        if (!context.requesterId().equals(userId) && !context.repairerId().equals(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        if (context.requesterId().equals(context.repairerId()))
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        return context;
    }
}
