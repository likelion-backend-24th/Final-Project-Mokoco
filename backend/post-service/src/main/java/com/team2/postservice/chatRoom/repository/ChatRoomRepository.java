package com.team2.postservice.chatRoom.repository;

import com.team2.postservice.chatRoom.dto.ChatRoomListItem;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from ChatRoom room where room.id = :id")
    Optional<ChatRoom> lockById(@Param("id") Long id);

    // 채택 전(제안 단계)에 만든 방은 room 자신의 postId/requesterId/repairerId를 쓰고,
    // 레거시(채택 시점에 만든) 방은 fixDeal 쪽 값으로 폴백한다 — left join 필수(안 그러면 새 방이 안 잡힘).
    @Query("""
            select new com.team2.postservice.chatRoom.dto.ChatRoomListItem(
                room.id, deal.id, coalesce(room.postId, deal.postId), post.title,
                case when coalesce(room.requesterId, deal.requesterId) = :userId
                     then coalesce(room.repairerId, deal.repairerId)
                     else coalesce(room.requesterId, deal.requesterId) end,
                message.content, message.createdAt, room.createdAt)
            from ChatRoom room
            left join room.fixDeal deal
            left join Post post on post.id = coalesce(room.postId, deal.postId)
            left join ChatMessage message on message.chatRoom.id = room.id
                and message.id = (select max(latest.id) from ChatMessage latest where latest.chatRoom.id = room.id)
            where coalesce(room.requesterId, deal.requesterId) = :userId
               or coalesce(room.repairerId, deal.repairerId) = :userId
            order by coalesce(message.createdAt, room.createdAt) desc, room.id desc
            """)
    List<ChatRoomListItem> findMyRooms(
            @Param("userId") Long userId,
            Pageable pageable);

    Optional<ChatRoom> findByFixDealId(Long fixDealId);

    boolean existsByFixDealId(Long fixDealId);

    Optional<ChatRoom> findByProposalId(Long proposalId);

    boolean existsByProposalId(Long proposalId);
}
