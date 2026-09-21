package com.team2.chatservice.chatRoom.service;

import com.team2.chatservice.chatRoom.dto.ChatRoomListItem;
import com.team2.chatservice.chatRoom.dto.ChatRoomResponse;
import com.team2.chatservice.chatRoom.entity.ChatRoom;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.client.PostClient;
import com.team2.chatservice.client.UserClient;
import com.team2.common.chat.ProposalChatResponse;
import com.team2.chatservice.client.dto.FixDealResponse;
import com.team2.chatservice.client.dto.UserClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
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
    private final UserClient userClient;
    private final PostClient postClient;
    private final org.springframework.transaction.PlatformTransactionManager transactions;

    public List<ChatRoomListItem> getMyRooms(String authorization, int page, int size) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (page < 0 || size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 50");
        UserClientResponse user;
        try {
            user = userClient.verifyToken(authorization.substring(7));
        } catch (feign.FeignException failure) {
            if (failure.status() == 401) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User authentication service unavailable");
        }
        if (user == null || user.id() == null)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid user authentication response");
        return chatRoomRepository.findMyRooms(user.id(), PageRequest.of(page, size));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatRoomResponse createChatRoom(Long fixDealId, String userEmail) {
        return createChatRoom(fixDealId, userClient.getUserByEmail(userEmail).id());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatRoomResponse createChatRoom(Long fixDealId, Long userId) {
        FixDealResponse deal = postClient.getFixDeal(fixDealId);
        if (deal == null || deal.proposalId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid post-service response");
        if (userId == null || !userId.equals(deal.requesterId()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_CREATE);
        return createForProposal(deal.proposalId(), userId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatRoomResponse createForProposal(Long proposalId, Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
        ChatRoomInfo room = postClient.ensureRoom(proposalId, userId);
        return new ChatRoomResponse(room.chatRoomId(), room.fixDealId(), room.proposalId(), room.createdAt());
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
        if (context.proposalId() == null || context.postId() == null || context.requesterId() == null
                || context.repairerId() == null || context.requesterId().equals(context.repairerId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid room context");
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
    public void syncProposal(Long proposalId) {
        Optional<ChatRoom> room = chatRoomRepository.findByProposalId(proposalId);
        if (room.isPresent()) refresh(room.get(), postClient.getProposalChat(proposalId));
    }

    private ChatRoomInfo info(ChatRoom room) {
        return new ChatRoomInfo(room.getId(), room.getFixDealId(), room.getProposalId(),
                room.getRequesterId(), room.getRepairerId(), room.getCreatedAt());
    }

    @Transactional
    public ChatRoomResponse getForProposal(Long proposalId, Long userId) {
        ProposalChatResponse context = authorizedProposal(proposalId, userId);
        return refresh(findProposalRoom(context)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)), context);
    }

    private ProposalChatResponse authorizedProposal(Long proposalId, Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
        ProposalChatResponse context = postClient.getProposalChat(proposalId);
        if (context == null || !proposalId.equals(context.proposalId()) || context.postId() == null
                || context.requesterId() == null || context.repairerId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid post-service response");
        if (!userId.equals(context.requesterId()) && !userId.equals(context.repairerId()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        if (context.requesterId().equals(context.repairerId()))
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE);
        return context;
    }

    private Optional<ChatRoom> findProposalRoom(ProposalChatResponse context) {
        return chatRoomRepository.findByProposalId(context.proposalId()).or(() ->
                context.fixDealId() == null ? Optional.empty() :
                        chatRoomRepository.findByFixDealId(context.fixDealId()));
    }

    private ChatRoomResponse refresh(ChatRoom room, ProposalChatResponse context) {
        if (!context.requesterId().equals(room.getRequesterId())
                || !context.repairerId().equals(room.getRepairerId()))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        room.updateContext(context.proposalId(), context.postId(), context.postTitle(), context.fixDealId());
        return ChatRoomResponse.from(room);
    }

    @Transactional
    public ChatRoomResponse detail(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.isParticipant(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        if (room.getProposalId() != null)
            return refresh(room, authorizedProposal(room.getProposalId(), userId));
        return ChatRoomResponse.from(room);
    }

    @Transactional
    public ChatRoomResponse getChatRoom(Long fixDealId, String userEmail) {
        FixDealResponse deal = postClient.getFixDeal(fixDealId);
        if (deal == null || deal.proposalId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid post-service response");
        UserClientResponse user = userClient.getUserByEmail(userEmail);
        if (user == null || user.id() == null
                || (!user.id().equals(deal.requesterId()) && !user.id().equals(deal.repairerId())))
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS);
        return getForProposal(deal.proposalId(), user.id());
    }
}
