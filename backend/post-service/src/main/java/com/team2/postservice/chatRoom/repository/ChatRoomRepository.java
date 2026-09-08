package com.team2.postservice.chatRoom.repository;

import com.team2.postservice.chatRoom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByFixDealId(Long fixDealId);

    boolean existsByFixDealId(Long fixDealId);
}