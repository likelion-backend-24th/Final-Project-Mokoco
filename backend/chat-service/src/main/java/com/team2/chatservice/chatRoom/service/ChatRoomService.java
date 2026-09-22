package com.team2.chatservice.chatRoom.service;

import com.team2.chatservice.chatRoom.dto.ChatRoomListItem;
import com.team2.chatservice.chatRoom.entity.ChatRoom;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.common.chat.ProposalChatResponse;
import com.team2.common.exception.CustomException;
import com.team2.chatservice.common.exception.ErrorCode;
import com.team2.common.chat.ChatRoomInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final org.springframework.transaction.PlatformTransactionManager transactions;

    public List<ChatRoomListItem> getMyRooms(Long userId, int page, int size) {
        if (userId == null || userId <= 0) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (page < 0 || size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        return chatRoomRepository.findMyRooms(userId, PageRequest.of(page, size));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatRoomInfo ensureInternal(ProposalChatResponse context) {
        org.springframework.transaction.support.TransactionTemplate transaction =
                new org.springframework.transaction.support.TransactionTemplate(transactions);
        try {
            return transaction.execute(status -> ensureInTransaction(context));
        } catch (DataIntegrityViolationException conflict) {
            // The failed insert transaction must finish before reading the winning room.
            return transaction.execute(status -> {
                ChatRoom room = findProposalRoom(context).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.CONFLICT, "Chat room context conflicted", conflict));
                refresh(room, context);
                return info(room);
            });
        }
    }

    private ChatRoomInfo ensureInTransaction(ProposalChatResponse context) {
        validate(context);
        Optional<ChatRoom> existing = findProposalRoom(context);
        if (existing.isPresent()) {
            refresh(existing.get(), context);
            return info(existing.get());
        }
        ChatRoom room = ChatRoom.create(context.fixDealId(), context.requesterId(), context.repairerId());
        refresh(room, context);
        return info(chatRoomRepository.saveAndFlush(room));
    }

    public ChatRoomInfo internalRoom(Long roomId) {
        return info(chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)));
    }

    @Transactional
    public ChatRoomInfo syncProposal(ProposalChatResponse context) {
        validate(context);
        ChatRoom room = findProposalRoom(context)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        refresh(room, context);
        return info(room);
    }

    private ChatRoomInfo info(ChatRoom room) {
        return new ChatRoomInfo(room.getId(), room.getFixDealId(), room.getProposalId(),
                room.getRequesterId(), room.getRepairerId(), room.getCreatedAt());
    }

    private Optional<ChatRoom> findProposalRoom(ProposalChatResponse context) {
        return chatRoomRepository.findByProposalId(context.proposalId()).or(() ->
                context.fixDealId() == null ? Optional.empty() :
                        chatRoomRepository.findByFixDealId(context.fixDealId()));
    }

    private void refresh(ChatRoom room, ProposalChatResponse context) {
        if (!context.requesterId().equals(room.getRequesterId())
                || !context.repairerId().equals(room.getRepairerId()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        room.updateContext(context.proposalId(), context.postId(), context.postTitle(), context.fixDealId());
    }

    private void validate(ProposalChatResponse context) {
        if (context == null || context.proposalId() == null || context.postId() == null
                || context.requesterId() == null || context.repairerId() == null
                || context.requesterId().equals(context.repairerId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid room context");
    }
}
