package com.team2.postservice.chatRoom;

import com.team2.common.exception.CustomException;
import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.contract.ContractRepository;
import com.team2.postservice.contract.RepairContract;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
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
    private final PostRepository postRepository;
    private final ContractRepository contractRepository;
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
        return ChatRoomResponse.from(room, dealStatus(proposal), proposal.getPost().getTitle());
    }

    public ChatRoomResponse getForProposal(Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        authorize(proposal, userId);
        return ChatRoomResponse.from(requireRoomByProposal(proposalId), dealStatus(proposal), proposal.getPost().getTitle());
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
        return ChatRoomResponse.from(room, deal.getStatus().name(), tryPostTitle(deal.getPostId()));
    }

    public ChatRoomResponse getForFixDeal(Long fixDealId, Long userId) {
        FixDeal deal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));
        if (!deal.getRequesterId().equals(userId) && !deal.getRepairerId().equals(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        return ChatRoomResponse.from(requireRoomByProposal(deal.getProposalId()), deal.getStatus().name(), tryPostTitle(deal.getPostId()));
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
        Long fixDealId = room.fixDealId() != null ? room.fixDealId() : repairFixDealLink(room);
        String dealStatus = fixDealId == null ? null
                : fixDealRepository.findById(fixDealId).map(deal -> deal.getStatus().name()).orElse(null);
        String contractStatus = contractRepository.findFirstByChatRoomIdOrderByRevisionDesc(roomId)
                .map(RepairContract::getStatus).orElse(null);
        // ChatRoomResponse.from(room, ...)은 room.fixDealId()를 그대로 쓰는데, 그건 chat-service에서
        // 막 읽어온 "복구 전" 값이다(repairFixDealLink가 chat-service에 다시 연결을 반영해도 이
        // 요청에서 이미 들고 있는 room 객체는 안 바뀐다). 방금 로컬에서 복구한 fixDealId로 직접
        // 채워야, 프론트가 이 응답 하나만 보고도(새로고침 한 번 더 없이) 계약서 버튼을 띄울 수 있다.
        return new ChatRoomResponse(room.id(), fixDealId, room.proposalId(), dealStatus, contractStatus,
                room.createdAt(), room.postId(), tryPostTitle(room.postId()));
    }

    // ProposalService.attachDealWithRetry()는 채택 직후 한 번만(최대 3회 재시도) 채팅방에 거래를
    // 연결한다 — 그 순간 아직 채팅방이 없었거나(제안 단계 채팅방을 나중에 연 경우) chat-service가
    // 일시적으로 응답하지 않았다면, 그 뒤로는 영영 연결이 안 된 채 남아 계약서 진입 버튼이 뜨지
    // 않는 문제가 있었다. 채팅방을 열 때마다(포커스 재조회 포함) 여기서 다시 확인해서, 이 제안에
    // 실제로는 거래가 있는데 채팅방에 안 걸려있으면 다시 연결을 시도한다.
    private Long repairFixDealLink(ChatRoomClient.ChatRoomInfo room) {
        if (room.proposalId() == null) return null;
        return fixDealRepository.findByProposalId(room.proposalId())
                .filter(deal -> deal.getStatus() != FixDealStatus.CANCELED)
                .map(deal -> {
                    try {
                        chatRoomClient.attachDeal(new ChatRoomClient.DealLinkRequest(
                                room.proposalId(), deal.getId(), deal.getRequesterId(), deal.getRepairerId(), deal.getPostId()));
                    } catch (Exception e) {
                        // chat-service에 반영은 이번에도 실패할 수 있지만, 이 응답만큼은 로컬 FixDeal
                        // 기준으로 정확하게 보여준다 — 다음 조회 때 다시 복구를 시도하면 된다.
                    }
                    return deal.getId();
                })
                .orElse(null);
    }

    // 채팅창 헤더에 글 제목 링크를 보여주기 위한 best-effort 조회 — 글이 삭제됐어도 채팅 자체는 봐야 하므로 실패는 삼킨다.
    private String tryPostTitle(Long postId) {
        if (postId == null) return null;
        return postRepository.findById(postId).map(Post::getTitle).orElse(null);
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
        UserClientResponse requester = userClient.getUserByEmail(proposal.getPost().getAuthorEmail());
        UserClientResponse repairer = userClient.getUserByEmail(proposal.getRepairerEmail());
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
