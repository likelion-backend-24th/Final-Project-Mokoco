package com.team2.postservice.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ContractService {
    public static final String CONSENT = "계약 내용과 금액, 작업 범위 및 조건을 확인했으며, 이 버전의 계약에 전자서명하는 것에 동의합니다.";
    private final ChatRoomRepository rooms;
    private final ContractRepository contracts;
    private final SignatureRepository signatures;
    private final ObjectMapper mapper;

    public record Version(Long id, int revision, String status, Long authorId, ContractTerms terms,
            String documentHash,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant createdAt,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant requestedAt,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant signedAt,
            List<ContractSignature> signatures) {}
    public record Overview(Long requesterId, Long repairerId, FixDealStatus dealStatus, String consentText, List<Version> versions) {}

    private ChatRoom participant(Long roomId, Long userId, boolean lock) {
        var room = (lock ? rooms.lockById(roomId) : rooms.findById(roomId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var deal = room.getFixDeal();
        if (!userId.equals(deal.getRequesterId()) && !userId.equals(deal.getRepairerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
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
    @Transactional(readOnly = true)
    public Overview get(Long roomId, Long userId) {
        var room = participant(roomId, userId, false);
        var deal = room.getFixDeal();
        return new Overview(deal.getRequesterId(), deal.getRepairerId(), deal.getStatus(), CONSENT,
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
        FixDealStatus from; FixDealStatus to;
        switch (action) {
            case "start" -> { from = FixDealStatus.MATCHED; to = FixDealStatus.REPAIRING; }
            case "finish" -> { from = FixDealStatus.REPAIRING; to = FixDealStatus.REPAIR_DONE; }
            case "accept" -> { from = FixDealStatus.REPAIR_DONE; to = FixDealStatus.COMPLETED; }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (deal.getStatus() != from) throw conflict("거래 상태가 변경되었습니다. 새로고침해주세요.");
        deal.changeStatus(to);
    }
}
