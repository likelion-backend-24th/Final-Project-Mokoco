package com.team2.postservice.ai.service;

import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.chatMessage.entity.*;
import com.team2.postservice.chatMessage.repository.ChatMessageRepository;
import com.team2.postservice.common.exception.AiException;
import com.team2.postservice.contract.ContractRepository;
import com.team2.postservice.fixDeal.entity.*;
import com.team2.postservice.post.entity.*;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiContractContextTest {
    final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    final ContractRepository contracts = mock(ContractRepository.class);
    final PostRepository posts = mock(PostRepository.class);
    final ProposalRepository proposals = mock(ProposalRepository.class);
    final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    final jakarta.persistence.EntityManager entityManager = mock(jakarta.persistence.EntityManager.class);
    final AiContractContext context = new AiContractContext(rooms,contracts,posts,proposals,messages,entityManager);
    ChatRoom room() {
        var room = ChatRoom.builder().id(1L).fixDeal(FixDeal.builder().postId(4L).proposalId(5L).requesterId(2L).repairerId(3L).build()).build();
        when(rooms.findById(1L)).thenReturn(Optional.of(room)); return room;
    }
    void source() {
        var post = mock(Post.class); when(post.getId()).thenReturn(4L); when(post.getTitle()).thenReturn("의자"); when(post.getContent()).thenReturn("다리 수리");
        when(posts.findById(4L)).thenReturn(Optional.of(post));
        var proposal = mock(Proposal.class); when(proposal.getPost()).thenReturn(post); when(proposal.getContent()).thenReturn("수리 제안");
        when(proposal.getEstimatedPrice()).thenReturn(50000);
        when(proposals.findById(5L)).thenReturn(Optional.of(proposal));
    }
    @Test void outsiderCannotReadSources() {
        room(); assertThatThrownBy(() -> context.read(1L,9L,null)).isInstanceOf(AiException.class);
        verifyNoInteractions(posts,proposals,messages);
    }
    @Test void loadsOnlyCurrentRoomTextQueryAndProposalAmount() {
        var room = room(); source();
        var message = ChatMessage.builder().id(8L).senderId(2L).chatRoom(room).messageType(MessageType.TEXT).content("다리만 고쳐주세요").build();
        when(messages.findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(eq(1L),eq(MessageType.TEXT),any())).thenReturn(List.of(message));
        var result = context.read(1L,2L,null);
        assertThat(result).containsEntry("PROPOSAL_AMOUNT","50000").containsEntry("MESSAGE_8","[의뢰인] 다리만 고쳐주세요");
        verify(messages).findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(eq(1L),eq(MessageType.TEXT),any());
    }
    @Test void rejectsMessagesFromAnotherRoomAndDeletedAttachments() {
        var room = room(); source();
        var wrong = ChatMessage.builder().id(8L).chatRoom(ChatRoom.builder().id(10L).build()).messageType(MessageType.TEXT).content("private").build();
        when(messages.findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(eq(1L),eq(MessageType.TEXT),any())).thenReturn(List.of(wrong));
        assertThatThrownBy(() -> context.read(1L,2L,null)).isInstanceOf(AiException.class);
        var deleted = ChatMessage.builder().id(8L).chatRoom(room).messageType(MessageType.TEXT).content("old").build(); deleted.delete();
        when(messages.findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(eq(1L),eq(MessageType.TEXT),any())).thenReturn(List.of(deleted));
        assertThatThrownBy(() -> context.read(1L,2L,null)).isInstanceOf(AiException.class);
    }
    @Test void rejectsStaleBaseAndInProgressDeal() {
        var room = room();
        assertThatThrownBy(() -> context.check(1L,2L,99L)).isInstanceOf(AiException.class);
        room.getFixDeal().changeStatus(FixDealStatus.REPAIRING);
        assertThatThrownBy(() -> context.check(1L,2L,null)).isInstanceOf(AiException.class);
        verify(entityManager,times(2)).clear();
    }
}
