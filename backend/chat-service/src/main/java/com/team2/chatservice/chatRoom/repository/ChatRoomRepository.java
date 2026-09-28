package com.team2.chatservice.chatRoom.repository;

import com.team2.chatservice.chatRoom.dto.ChatRoomListItem;
import com.team2.chatservice.chatRoom.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from ChatRoom room where room.id = :id")
    Optional<ChatRoom> lockById(@Param("id") Long id);

    // Post는 post-service가 소유한 엔티티라 여기서는 JPA 매핑을 못 하므로, 같은 DB의 posts 테이블을
    // 네이티브 SQL로 직접 조회한다(쓰기는 안 함 — 읽기 전용 same-DB 커플링).
    @Query(value = """
            select
                cr.id as chatRoomId,
                cr.fix_deal_id as fixDealId,
                cr.post_id as postId,
                p.title as postTitle,
                case when cr.requester_id = :userId then cr.repairer_id else cr.requester_id end as counterpartId,
                m.content as lastMessage,
                m.created_at as lastMessageAt,
                cr.created_at as createdAt
            from chat_rooms cr
            left join posts p on p.id = cr.post_id
            left join chat_messages m on m.id = (
                select max(m2.id) from chat_messages m2 where m2.chat_room_id = cr.id
            )
            where cr.requester_id = :userId or cr.repairer_id = :userId
            order by coalesce(m.created_at, cr.created_at) desc, cr.id desc
            """, nativeQuery = true)
    List<ChatRoomListItem> findMyRooms(@Param("userId") Long userId, Pageable pageable);

    Optional<ChatRoom> findByFixDealId(Long fixDealId);

    boolean existsByFixDealId(Long fixDealId);

    Optional<ChatRoom> findByProposalId(Long proposalId);

    boolean existsByProposalId(Long proposalId);

    List<ChatRoom> findByFixDealIdIn(List<Long> fixDealIds);
}
