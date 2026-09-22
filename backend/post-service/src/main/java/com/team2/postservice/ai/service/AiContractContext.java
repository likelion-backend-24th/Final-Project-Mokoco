package com.team2.postservice.ai.service;

import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.common.exception.AiException;
import com.team2.postservice.contract.ContractRepository;
import com.team2.postservice.contract.RepairContract;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.post.service.PostContent;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiContractContext {
    private final ChatRoomClient chatRoomClient;
    private final FixDealRepository fixDeals;
    private final ContractRepository contracts;
    private final PostRepository posts;
    private final ProposalRepository proposals;

    @Transactional(readOnly = true)
    public void check(Long roomId, Long userId, Long baseId) {
        checkedDeal(roomId, userId, baseId);
    }

    private FixDeal checkedDeal(Long roomId, Long userId, Long baseId) {
        ChatRoomClient.ChatRoomInfo room;
        try {
            room = chatRoomClient.getRoom(roomId);
        } catch (FeignException.NotFound e) {
            throw new AiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다.");
        } catch (FeignException e) {
            throw new AiException(HttpStatus.BAD_GATEWAY, "CHAT_SERVICE_UNAVAILABLE", "채팅방 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
        if (!userId.equals(room.requesterId()) && !userId.equals(room.repairerId()))
            throw new AiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "이 거래의 참여자만 사용할 수 있습니다.");
        if (room.fixDealId() == null)
            throw new AiException(HttpStatus.CONFLICT, "PROPOSAL_NOT_ADOPTED", "견적 채택 후 계약 초안을 작성할 수 있습니다.");
        FixDeal deal = fixDeals.findById(room.fixDealId())
                .orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."));
        RepairContract latest = contracts.findFirstByChatRoomIdOrderByRevisionDesc(roomId).orElse(null);
        if (deal.getStatus() != FixDealStatus.MATCHED || deal.getRequesterId().equals(deal.getRepairerId())
                || !Objects.equals(baseId, latest == null ? null : latest.getId())
                || (latest != null && "SIGNED".equals(latest.getStatus())))
            throw new AiException(HttpStatus.CONFLICT, "CONTRACT_CHANGED", "계약 또는 거래 상태가 변경되었습니다. 최신 내용을 확인해주세요.");
        return deal;
    }

    @Transactional(readOnly = true)
    public Map<String, String> read(Long roomId, Long userId, Long baseId) {
        FixDeal deal = checkedDeal(roomId, userId, baseId);
        Post post = posts.findById(deal.getPostId()).orElseThrow(() -> AiException.input("의뢰 내용을 찾을 수 없습니다."));
        Proposal proposal = proposals.findById(deal.getProposalId()).orElseThrow(() -> AiException.input("견적 내용을 찾을 수 없습니다."));
        if (!Objects.equals(proposal.getPost().getId(), post.getId())) throw AiException.input("거래에 연결된 견적을 확인해주세요.");
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("POST", post.getTitle() + "\n" + PostContent.sanitize(post.getContent(), post.getContentFormat()));
        if (proposal.getEstimatedPrice() == null || proposal.getEstimatedPrice() <= 0)
        sources.put("POST", post.getTitle() + "\n" + PostContent.plainText(post.getContent(), post.getContentFormat()));

        if (proposal.getEstimatedPrice() == null || proposal.getEstimatedPrice() <= 0){
            throw AiException.input("채택된 제안의 금액을 확인해주세요.");
        sources.put("ADOPTED_PROPOSAL", proposal.getContent());
        sources.put("PROPOSAL_AMOUNT", proposal.getEstimatedPrice().toString());
        List<ChatRoomClient.TextMessage> history;
        try {
            history = chatRoomClient.textMessages(roomId, 501);
        } catch (FeignException e) {
            throw new AiException(HttpStatus.BAD_GATEWAY, "CHAT_SERVICE_UNAVAILABLE", "대화 내용을 확인할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
        if (history.size() > 500) throw AiException.input("텍스트 대화가 500개를 넘어 자동 정리할 수 없습니다. 계약 내용을 직접 작성해주세요.");
        for (ChatRoomClient.TextMessage message : history) {
            String role = Objects.equals(message.senderId(), deal.getRequesterId()) ? "의뢰인" : "수리자";
            sources.put("MESSAGE_" + message.id(), "[" + role + "] " + message.content());
        }
        return sources;
    }
}
