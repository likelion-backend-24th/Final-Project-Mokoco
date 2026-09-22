package com.team2.postservice.contract;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "repair_contract_versions", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "revision"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RepairContract {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "chat_room_id", nullable = false) private Long chatRoomId;
    @Column(nullable = false) private int revision;
    @Column(nullable = false) private Long authorId;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String termsJson;
    @Column(nullable = false, length = 64) private String documentHash;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private Instant createdAt;
    private Instant requestedAt;
    private Instant signedAt;

    public RepairContract(Long roomId, int revision, Long authorId, String json, String hash) {
        this.chatRoomId = roomId; this.revision = revision; this.authorId = authorId;
        this.termsJson = json; this.documentHash = hash; this.status = "DRAFT"; this.createdAt = Instant.now();
    }
    public void requestSignatures() { status = "SIGNING"; requestedAt = Instant.now(); }
    public void supersede() { status = "SUPERSEDED"; }
    public void complete() { status = "SIGNED"; signedAt = Instant.now(); }
}
