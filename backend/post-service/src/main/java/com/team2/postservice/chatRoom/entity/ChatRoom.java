package com.team2.postservice.chatRoom.entity;

import com.team2.postservice.fixDeal.entity.FixDeal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fix_deal_id", nullable = false, unique = true)
    private FixDeal fixDeal;

    private LocalDateTime createdAt;


}
