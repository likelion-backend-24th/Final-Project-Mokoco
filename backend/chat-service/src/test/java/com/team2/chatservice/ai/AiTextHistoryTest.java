package com.team2.chatservice.ai;

import com.team2.chatservice.chatMessage.entity.*;
import com.team2.chatservice.chatMessage.repository.ChatMessageRepository;
import com.team2.chatservice.chatRoom.entity.ChatRoom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
class AiTextHistoryTest {
    @Autowired TestEntityManager em;
    @Autowired ChatMessageRepository messages;
    ChatRoom room(long post) {

        return em.persist(ChatRoom.create(post, 10L, 20L));
    }

    ChatMessage message(ChatRoom room, MessageType type, String text) {
        return em.persist(ChatMessage.builder().chatRoom(room).senderId(10L).messageType(type).content(text).build());
    }

    @Test void queryExcludesMediaSystemDeletedAndOtherRoomAndPreservesOrder() {
        ChatRoom room = room(1);
        ChatRoom other = room(2);

        ChatMessage first = message(room,MessageType.TEXT,"첫 합의");

        message(room,MessageType.IMAGE,"사진 설명"); message(room,MessageType.VIDEO,"영상 설명");
        message(room,MessageType.SYSTEM,"시스템 메시지");
        message(room,MessageType.TEXT,"삭제 예정").delete();
        message(other,MessageType.TEXT,"다른 방의 비공개 내용");

        ChatMessage last = message(room,MessageType.TEXT,"변경 합의");

        em.flush(); em.clear();

        List<ChatMessage> result = messages.findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(room.getId(),MessageType.TEXT,PageRequest.of(0,501));
        assertThat(result).extracting(ChatMessage::getId).containsExactly(first.getId(),last.getId());
    }
}
