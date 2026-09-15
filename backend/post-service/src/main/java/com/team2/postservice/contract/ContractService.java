package com.team2.postservice.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.dto.PaymentClientResponse;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.repository.ProposalRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {
    public static final String CONSENT = "계약 내용과 금액, 작업 범위 및 조건을 확인했으며, 이 버전의 계약에 전자서명하는 것에 동의합니다.";
    private final ChatRoomRepository rooms;
    private final ContractRepository contracts;
    private final SignatureRepository signatures;
    private final PostRepository posts;
    private final ProposalRepository proposals;
    private final PaymentClient paymentClient;
    private final ObjectMapper mapper;

    public record Version(Long id, int revision, String status, Long authorId, ContractTerms terms,
            String documentHash,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant createdAt,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant requestedAt,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant signedAt,
            List<ContractSignature> signatures) {}
    public record PaymentSummary(String status, Integer amount, Integer feeAmount, Integer netAmount, boolean settled) {}
    public record Overview(Long requesterId, Long repairerId, String requesterEmail, String repairerEmail,
            Long postId, Long fixDealId, Integer estimatedPrice, FixDealStatus dealStatus,
            PaymentSummary payment, String consentText, List<Version> versions) {}

    private ChatRoom participant(Long roomId, Long userId, boolean lock) {
        var room = (lock ? rooms.lockById(roomId) : rooms.findById(roomId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!room.hasParticipant(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        // 채택 전(제안 단계) 채팅방은 FixDeal이 아직 없다 — 계약은 채택 후에만 가능하다.
        if (room.getFixDeal() == null)
            throw conflict("견적 채택 후 계약서를 작성할 수 있습니다.");
        return room;
    }
    private Version view(RepairContract contract) {
        try {
            return new Version(contract.getId(), contract.getRevision(), contract.getStatus(), contract.getAuthorId(),
                    mapper.readValue(contract.getTermsJson(), ContractTerms.class), contract.getDocumentHash(),
                    contract.getCreatedAt(), contract.getRequestedAt(), contract.getSignedAt(),
                    signatures.findByContractIdOrderBySignedAtAsc(contract.getId()));
        } catch (java.io.IOException e) { throw new IllegalStateException("Stored contract cannot be read", e); }
    }
    // 결제 상태 조회 실패로 계약서 페이지 전체가 죽으면 안 되므로, 조회용으로는 어떤 Feign
    // 오류든 "아직 결제 없음"으로 취급하고 페이지는 계속 보여준다.
    private PaymentSummary paymentSummaryOrNull(Long postId, String requesterEmail) {
        try {
            PaymentClientResponse payment = paymentClient.getPaymentByPostId(postId, requesterEmail);
            return new PaymentSummary(payment.status(), payment.amount(), payment.feeAmount(),
                    payment.netAmount(), payment.settledAt() != null);
        } catch (FeignException.NotFound e) {
            return null;
        } catch (FeignException e) {
            log.warn("결제 상태 조회 실패 postId={}", postId, e);
            return null;
        }
    }
    @Transactional(readOnly = true)
    public Overview get(Long roomId, Long userId) {
        var room = participant(roomId, userId, false);
        var deal = room.getFixDeal();
        Post post = posts.findById(deal.getPostId()).orElse(null);
        var proposal = proposals.findById(deal.getProposalId()).orElse(null);
        String requesterEmail = post != null ? post.getAuthorEmail() : null;
        String repairerEmail = proposal != null ? proposal.getRepairerEmail() : null;
        Integer estimatedPrice = proposal != null ? proposal.getEstimatedPrice() : null;
        PaymentSummary payment = requesterEmail == null ? null : paymentSummaryOrNull(deal.getPostId(), requesterEmail);
        return new Overview(deal.getRequesterId(), deal.getRepairerId(), requesterEmail, repairerEmail,
                deal.getPostId(), deal.getId(), estimatedPrice, deal.getStatus(), payment, CONSENT,
                contracts.findByChatRoomIdOrderByRevisionDesc(roomId).stream().map(this::view).toList());
    }
    private RepairContract latest(Long roomId, Long expectedId) {
        var current = contracts.findFirstByChatRoomIdOrderByRevisionDesc(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!current.getId().equals(expectedId)) throw conflict("계약이 변경되었습니다. 최신 버전을 확인해주세요.");
        return current;
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }

    @Transactional
    public Version draft(Long roomId, Long userId, Long baseId, ContractTerms terms) {
        var room = participant(roomId, userId, true);
        if (room.getFixDeal().getRequesterId().equals(room.getFixDeal().getRepairerId()))
            throw conflict("서로 다른 두 당사자만 계약할 수 있습니다.");
        if (room.getFixDeal().getStatus() != FixDealStatus.MATCHED) throw conflict("진행 중인 거래는 계약을 새로 작성할 수 없습니다.");
        if (terms.endDate().isBefore(terms.startDate())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "완료일은 시작일 이후여야 합니다.");
        var previous = contracts.findFirstByChatRoomIdOrderByRevisionDesc(roomId).orElse(null);
        if (previous == null ? baseId != null : !previous.getId().equals(baseId)) throw conflict("최신 계약을 다시 불러와주세요.");
        if (previous != null && previous.getStatus().equals("SIGNED")) throw conflict("체결된 계약은 수정할 수 없습니다.");
        int revision = previous == null ? 1 : previous.getRevision() + 1;
        try {
            String json = mapper.writeValueAsString(terms);
            String canonical = "repair-contract-v1\n" + roomId + "\n" + room.getFixDeal().getRequesterId() + "\n"
                    + room.getFixDeal().getRepairerId() + "\n" + revision + "\n" + json;
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
            if (previous != null) previous.supersede();
            return view(contracts.saveAndFlush(new RepairContract(roomId, revision, userId, json, hash)));
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Transactional
    public Version request(Long roomId, Long userId, Long id) {
        var room = participant(roomId, userId, true);
        if (room.getFixDeal().getStatus() != FixDealStatus.MATCHED) throw conflict("계약 요청이 가능한 거래 상태가 아닙니다.");
        var contract = latest(roomId, id);
        if (!contract.getStatus().equals("DRAFT")) throw conflict("초안만 서명 요청할 수 있습니다.");
        contract.requestSignatures(); return view(contract);
    }
    @Transactional
    public Version sign(Long roomId, Long userId, Long id, String hash, String name, boolean consent) {
        var room = participant(roomId, userId, true);
        if (room.getFixDeal().getStatus() != FixDealStatus.MATCHED) throw conflict("서명 가능한 거래 상태가 아닙니다.");
        var contract = latest(roomId, id);
        if (!consent || name == null || name.isBlank() || name.length() > 80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "성명과 서명 동의가 필요합니다.");
        if (!contract.getDocumentHash().equals(hash)) throw conflict("서명 대상 내용이 변경되었습니다.");
        if (!contract.getStatus().equals("SIGNING") && !contract.getStatus().equals("SIGNED")) throw conflict("서명 요청된 계약만 서명할 수 있습니다.");
        var existing = signatures.findByContractIdOrderBySignedAtAsc(id);
        if (existing.stream().anyMatch(s -> s.getSignerId().equals(userId))) return view(contract);
        if (contract.getStatus().equals("SIGNED")) throw conflict("이미 체결된 계약입니다.");
        signatures.saveAndFlush(new ContractSignature(id, userId, name.trim(), hash, CONSENT));
        var signers = signatures.findByContractIdOrderBySignedAtAsc(id).stream().map(ContractSignature::getSignerId).toList();
        if (signers.contains(room.getFixDeal().getRequesterId()) && signers.contains(room.getFixDeal().getRepairerId())) contract.complete();
        return view(contract);
    }
    @Transactional
    public void advance(Long roomId, Long userId, Long id, String action) {
        var room = participant(roomId, userId, true);
        if (!latest(roomId, id).getStatus().equals("SIGNED")) throw conflict("양측 서명 완료 후 진행할 수 있습니다.");
        var deal = room.getFixDeal();
        boolean requester = action.equals("accept");
        if (!userId.equals(requester ? deal.getRequesterId() : deal.getRepairerId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        switch (action) {
            case "start" -> {
                if (deal.getStatus() != FixDealStatus.MATCHED) throw conflict("거래 상태가 변경되었습니다. 새로고침해주세요.");
                Post post = posts.findById(deal.getPostId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                PaymentClientResponse payment = requirePayment(deal.getPostId(), post.getAuthorEmail());
                if (!"COMPLETED".equals(payment.status()))
                    throw conflict("의뢰인의 결제가 완료되어야 작업을 시작할 수 있습니다.");
                deal.changeStatus(FixDealStatus.REPAIRING);
            }
            case "finish" -> {
                // PRODUCT_SENT는 결제를 미리 받는 새 흐름 도입 전 레거시 상태 — 그 상태에 남아있는
                // 기존 거래도 계속 진행할 수 있도록 finish의 출발 상태로 함께 허용한다.
                if (deal.getStatus() != FixDealStatus.REPAIRING && deal.getStatus() != FixDealStatus.PRODUCT_SENT)
                    throw conflict("거래 상태가 변경되었습니다. 새로고침해주세요.");
                deal.changeStatus(FixDealStatus.REPAIR_DONE);
            }
            case "accept" -> {
                if (deal.getStatus() != FixDealStatus.REPAIR_DONE) throw conflict("거래 상태가 변경되었습니다. 새로고침해주세요.");
                Post post = posts.findById(deal.getPostId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                deal.changeStatus(FixDealStatus.COMPLETED);
                // 거래가 최종 완료됐으니 원글 상태도 같이 '거래 완료'로 넘긴다.
                post.updateStatusToCompleted();
                // 정산 확정 — 이 호출이 실패하면 트랜잭션 전체가 롤백되어 위 상태 전환도 함께
                // 취소된다. settle()은 멱등이라 사용자가 버튼을 다시 눌러 안전하게 재시도할 수 있다.
                try {
                    paymentClient.settle(deal.getPostId(), post.getAuthorEmail());
                } catch (FeignException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "정산 처리에 실패했습니다. 잠시 후 다시 시도해주세요.");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private PaymentClientResponse requirePayment(Long postId, String requesterEmail) {
        try {
            return paymentClient.getPaymentByPostId(postId, requesterEmail);
        } catch (FeignException.NotFound e) {
            throw conflict("의뢰인의 결제가 완료되어야 작업을 시작할 수 있습니다.");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "결제 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
