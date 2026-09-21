package com.team2.postservice.proposal.entity;

import com.team2.postservice.post.entity.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proposals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false)
    private Long repairerId; // 수리공 사용자 ID

    @Column(nullable = false)
    private Integer estimatedPrice; // 희망 견적 금액

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 수리 제안 및 견적 내용

    @Column(nullable = false)
    private boolean isAdopted = false; // 채택 여부

    @Column(nullable = false)
    private boolean attachResume = false;

    @Builder
    public Proposal(Post post, int estimatedPrice, Long repairerId, String content, boolean attachResume) {
        this.post = post;
        this.repairerId = repairerId;
        this.estimatedPrice = estimatedPrice;
        this.content = content;
        this.isAdopted = false;
        this.attachResume = attachResume;
    }

    public void adopt() {
        this.isAdopted = true;
    }

    public void cancel() {
        this.isAdopted = false;
    }
}