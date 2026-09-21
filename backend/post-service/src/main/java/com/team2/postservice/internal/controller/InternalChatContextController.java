package com.team2.postservice.internal.controller;

import com.team2.common.chat.ChatRoomInfo;
import com.team2.common.chat.ProposalChatResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import com.team2.postservice.client.ChatClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal")
@Transactional(readOnly = true)
public class InternalChatContextController {
    private final FixDealRepository deals;
    private final ProposalRepository proposals;
    private final UserClient users;
    private final ChatClient chat;
    private final byte[] serviceKey;

    public InternalChatContextController(FixDealRepository deals, ProposalRepository proposals,
            UserClient users, ChatClient chat, @Value("${internal.service-key}") String serviceKey) {
        if (serviceKey.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        this.deals = deals;
        this.proposals = proposals;
        this.users = users;
        this.chat = chat;
        this.serviceKey = serviceKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/fix-deals/{fixDealId}")
    public DealContext deal(@PathVariable Long fixDealId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        FixDeal deal = deals.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));
        return new DealContext(deal.getId(), deal.getPostId(), deal.getProposalId(),
                deal.getRequesterId(), deal.getRepairerId());
    }

    @GetMapping("/proposals/{proposalId}/chat")
    public ProposalChatResponse proposal(@PathVariable Long proposalId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        return context(proposals.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND)));
    }

    @PostMapping("/proposals/{proposalId}/chat-room")
    @Transactional
    public ChatRoomInfo ensureRoom(@PathVariable Long proposalId, @RequestParam Long userId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        // Keep the same proposal lock as deletion until the remote room has committed.
        Proposal proposal = proposals.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        ProposalChatResponse context = context(proposal);
        if (!userId.equals(context.requesterId()) && !userId.equals(context.repairerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (context.requesterId().equals(context.repairerId()))
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        return chat.ensureRoom(context);
    }

    private ProposalChatResponse context(Proposal proposal) {
        FixDeal deal = proposal.isAdopted() ? deals.findByProposalId(proposal.getId()).orElse(null) : null;
        Long requesterId = deal == null ? users.getUserByEmail(proposal.getPost().getAuthorEmail()).id() : deal.getRequesterId();
        Long repairerId = deal == null ? users.getUserByEmail(proposal.getRepairerEmail()).id() : deal.getRepairerId();
        return new ProposalChatResponse(proposal.getId(), proposal.getPost().getId(), proposal.getPost().getTitle(),
                requesterId, repairerId, deal == null ? null : deal.getId());
    }

    private void authenticate(String key) {
        if (key == null || !MessageDigest.isEqual(serviceKey, key.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    public record DealContext(Long id, Long postId, Long proposalId, Long requesterId, Long repairerId) {}
}
