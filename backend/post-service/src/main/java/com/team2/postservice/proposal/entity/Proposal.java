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
    private Long repairerId; // 수리공 유저 아이디 넘버

    @Column
    private String repairerEmail;

    @Column(nullable = false)
    private Integer estimatedPrice; // 희망 견적 금액

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 수리 제안 및 견적 내용

    @Column(nullable = false)
    private boolean isAdopted = false; // 채택 여부

    @Column(nullable = false)
    private boolean attachResume = false; // 이 제안에 내 이력서를 보여줄지 여부

    @Builder
    public Proposal(Post post, int estimatedPrice, Long repairerId, String repairerEmail, String content, boolean attachResume) {
        this.post = post;
        this.repairerId = repairerId;
        this.repairerEmail = repairerEmail;
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