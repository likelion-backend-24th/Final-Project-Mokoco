package com.team2.chatservice.internal.dto;

// post-service가 이미 로컬에 갖고 있는 Proposal 컨텍스트를 넘기면, 그 제안 기준 방을 찾거나
// 없으면 새로 만든다. proposal_id에 DB unique 제약이 있어 동시에 두 요청이 들어오면 하나는
// 제약 위반으로 실패하는데, 그 경우 예외를 삼키고 방금 다른 요청이 만든 방을 그대로 반환한다.
public record ProposalRoomContext(Long proposalId, Long postId, Long requesterId, Long repairerId) {}
