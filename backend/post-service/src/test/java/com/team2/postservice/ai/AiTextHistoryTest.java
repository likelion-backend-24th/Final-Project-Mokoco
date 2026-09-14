package com.team2.postservice.ai;

import com.team2.postservice.chatMessage.entity.*;
import com.team2.postservice.chatMessage.repository.ChatMessageRepository;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.fixDeal.entity.FixDeal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class AiTextHistoryTest {
    @Autowired TestEntityManager em;
    @Autowired ChatMessageRepository messages;
    ChatRoom room(long post) {
        var deal = em.persist(FixDeal.builder().postId(post).proposalId(post).requesterId(10L).repairerId(20L).build());
        return em.persist(ChatRoom.builder().fixDeal(deal).build());
    }
    ChatMessage message(ChatRoom room, MessageType type, String text) {
        return em.persist(ChatMessage.builder().chatRoom(room).senderId(10L).messageType(type).content(text).build());
    }
    @Test void queryExcludesMediaSystemDeletedAndOtherRoomAndPreservesOrder() {
        var room = room(1); var other = room(2);
        var first = message(room,MessageType.TEXT,"첫 합의");
        message(room,MessageType.IMAGE,"사진 설명"); message(room,MessageType.VIDEO,"영상 설명");
        message(room,MessageType.SYSTEM,"시스템 메시지");
        message(room,MessageType.TEXT,"삭제 예정").delete();
        message(other,MessageType.TEXT,"다른 방의 비공개 내용");
        var last = message(room,MessageType.TEXT,"변경 합의");
        em.flush(); em.clear();
        var result = messages.findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(room.getId(),MessageType.TEXT,PageRequest.of(0,501));
        assertThat(result).extracting(ChatMessage::getId).containsExactly(first.getId(),last.getId());
    }
}
