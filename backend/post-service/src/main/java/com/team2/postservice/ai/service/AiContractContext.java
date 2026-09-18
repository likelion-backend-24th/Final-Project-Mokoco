package com.team2.postservice.ai.service;

import com.team2.postservice.chatMessage.entity.ChatMessage;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.chatMessage.repository.ChatMessageRepository;
import com.team2.postservice.chatMessage.entity.MessageType;
import com.team2.postservice.common.exception.AiException;
import com.team2.postservice.contract.repository.ContractRepository;
import com.team2.postservice.contract.entity.RepairContract;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiContractContext {
    private final ChatRoomRepository rooms;
    private final ContractRepository contracts;
    private final PostRepository posts;
    private final ProposalRepository proposals;
    private final ChatMessageRepository messages;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional(readOnly = true)
    public void check(Long roomId, Long userId, Long baseId) {
        // OSIV may retain the earlier read's entities across the provider call.
        // Discard that read-only snapshot before checking the current database state.
        entityManager.clear();
        checkedRoom(roomId, userId, baseId);
    }

    private ChatRoom checkedRoom(Long roomId, Long userId, Long baseId) {

        ChatRoom room = rooms.findById(roomId).orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."));
        FixDeal deal = room.getFixDeal();

        if (!room.hasParticipant(userId)){
            throw new AiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "이 거래의 참여자만 사용할 수 있습니다.");
        }

        if (deal == null) {
            throw new AiException(HttpStatus.CONFLICT, "PROPOSAL_NOT_ADOPTED", "견적 채택 후 계약 초안을 작성할 수 있습니다.");
        }

        RepairContract latest = contracts.findFirstByChatRoomIdOrderByRevisionDesc(roomId).orElse(null);

        if (deal.getStatus() != FixDealStatus.MATCHED || deal.getRequesterId().equals(deal.getRepairerId())
                || !Objects.equals(baseId, latest == null ? null : latest.getId())
                || (latest != null && "SIGNED".equals(latest.getStatus())))
        {
            throw new AiException(HttpStatus.CONFLICT, "CONTRACT_CHANGED", "계약 또는 거래 상태가 변경되었습니다. 최신 내용을 확인해주세요.");
        }

        return room;
    }

    @Transactional(readOnly = true)
    public Map<String, String> read(Long roomId, Long userId, Long baseId) {

        FixDeal deal = checkedRoom(roomId, userId, baseId).getFixDeal();
        Post post = posts.findById(deal.getPostId()).orElseThrow(() -> AiException.input("의뢰 내용을 찾을 수 없습니다."));
        Proposal proposal = proposals.findById(deal.getProposalId()).orElseThrow(() -> AiException.input("견적 내용을 찾을 수 없습니다."));

        if (!Objects.equals(proposal.getPost().getId(), post.getId())) {
            throw AiException.input("거래에 연결된 견적을 확인해주세요.");
        }

        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("POST", post.getTitle() + "\n" + post.getContent());

        if (proposal.getEstimatedPrice() == null || proposal.getEstimatedPrice() <= 0){
            throw AiException.input("채택된 제안의 금액을 확인해주세요.");
        }

        sources.put("ADOPTED_PROPOSAL", proposal.getContent());
        sources.put("PROPOSAL_AMOUNT", proposal.getEstimatedPrice().toString());

        List<ChatMessage> history = messages.findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(
                roomId, MessageType.TEXT, org.springframework.data.domain.PageRequest.of(0, 501));

        if (history.size() > 500) {
            throw AiException.input("텍스트 대화가 500개를 넘어 자동 정리할 수 없습니다. 계약 내용을 직접 작성해주세요.");
        }

        for (ChatMessage message : history) {
            if (!roomId.equals(message.getChatRoom().getId()) || message.getDeletedAt() != null || message.getMessageType() != MessageType.TEXT){
                throw AiException.input("채팅방 대화 정보를 확인하지 못했습니다.");
            }
            String role = Objects.equals(message.getSenderId(), deal.getRequesterId()) ? "의뢰인" : "수리자";
            sources.put("MESSAGE_" + message.getId(), "[" + role + "] " + message.getContent());
        }

        return sources;
    }
}
