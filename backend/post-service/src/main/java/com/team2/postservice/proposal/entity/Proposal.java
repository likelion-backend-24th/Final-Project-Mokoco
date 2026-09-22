package com.team2.postservice.proposal.entity;

import com.team2.postservice.post.entity.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "proposals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false)
    private String repairerEmail; // 수리공 이메일

    @Column(nullable = false)
    private Integer estimatedPrice; // 희망 견적 금액

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 수리 제안 및 견적 내용

    @Column(nullable = false)
    private boolean isAdopted = false; // 채택 여부

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Proposal(Post post, int estimatedPrice, String repairerEmail, String content) {
        this.post = post;
        this.repairerEmail = repairerEmail;
        this.estimatedPrice = estimatedPrice;
        this.content = content;
        this.isAdopted = false;
    }

    public void adopt() {
        this.isAdopted = true;
    }
}