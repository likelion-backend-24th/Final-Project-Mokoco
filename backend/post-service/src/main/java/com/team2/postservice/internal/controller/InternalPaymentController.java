package com.team2.postservice.internal.controller;

import com.team2.common.payment.PaymentContext;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/payments")
public class InternalPaymentController {
    private final FixDealRepository deals;
    private final ProposalRepository proposals;
    private final byte[] key;
    public InternalPaymentController(FixDealRepository deals, ProposalRepository proposals,
                                     @Value("${internal.service-key}") String key) {
        this.deals = deals; this.proposals = proposals;
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }
    @GetMapping("/posts/{postId}")
    @Transactional(readOnly = true)
    public PaymentContext context(@PathVariable Long postId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String supplied) {
        if (supplied == null || !MessageDigest.isEqual(key, supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        FixDeal deal = deals.findByPostId(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Proposal proposal = proposals.findById(deal.getProposalId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!proposal.isAdopted() || !proposal.getPost().getId().equals(postId) || proposal.getEstimatedPrice() <= 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        return new PaymentContext(postId, deal.getId(), deal.getRequesterId(), proposal.getPost().getAuthorEmail(),
                proposal.getRepairerEmail(), proposal.getEstimatedPrice(), deal.getStatus().name());
    }
}
