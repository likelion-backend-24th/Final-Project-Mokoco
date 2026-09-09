package com.team2.postservice.chatRoom.repository;

import com.team2.postservice.chatRoom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @org.springframework.data.jpa.repository.Query("""
            select new com.team2.postservice.chatRoom.dto.ChatRoomListItem(
                room.id, deal.id, deal.postId, post.title,
                case when deal.requesterId = :userId then deal.repairerId else deal.requesterId end,
                message.content, message.createdAt, room.createdAt)
            from ChatRoom room
            join room.fixDeal deal
            left join Post post on post.id = deal.postId
            left join ChatMessage message on message.chatRoom.id = room.id
                and message.id = (select max(latest.id) from ChatMessage latest where latest.chatRoom.id = room.id)
            where deal.requesterId = :userId or deal.repairerId = :userId
            order by coalesce(message.createdAt, room.createdAt) desc, room.id desc
            """)
    java.util.List<com.team2.postservice.chatRoom.dto.ChatRoomListItem> findMyRooms(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    Optional<ChatRoom> findByFixDealId(Long fixDealId);

    boolean existsByFixDealId(Long fixDealId);
}
