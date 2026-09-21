package com.team2.chatservice.chatRoom.repository;

import com.team2.chatservice.chatRoom.dto.ChatRoomListItem;
import com.team2.chatservice.chatRoom.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from ChatRoom room where room.id = :id")
    Optional<ChatRoom> lockById(@Param("id") Long id);

    @Query("""
            select new com.team2.chatservice.chatRoom.dto.ChatRoomListItem(
                room.id, room.fixDealId, room.postId, room.postTitle,
                case when room.requesterId = :userId then room.repairerId else room.requesterId end,
                message.content, message.createdAt, room.createdAt)
            from ChatRoom room
            left join ChatMessage message on message.chatRoom.id = room.id
                and message.id = (select max(latest.id) from ChatMessage latest where latest.chatRoom.id = room.id)
            where room.requesterId = :userId or room.repairerId = :userId
            order by coalesce(message.createdAt, room.createdAt) desc, room.id desc
            """)
    List<ChatRoomListItem> findMyRooms(
            @Param("userId") Long userId,
            Pageable pageable);

    Optional<ChatRoom> findByProposalId(Long proposalId);

    boolean existsByProposalId(Long proposalId);

    Optional<ChatRoom> findByFixDealId(Long fixDealId);

    List<ChatRoom> findByFixDealIdIn(List<Long> fixDealIds);

    boolean existsByFixDealId(Long fixDealId);
}
