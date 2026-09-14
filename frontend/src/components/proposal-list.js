"use client";

import { useState } from "react";
import { CheckCircle, Trash, MapPin, Wrench, CaretDown, CaretUp } from "@phosphor-icons/react";
import { useRouter } from "next/navigation";
import ProposalChatRoom from "@/components/proposal-chat-room";
import FixDealProgress from "@/components/fix-deal-progress";

export default function ProposalList({ postId, proposals: initialProposals, isMine, userEmail }) {
  const [proposals, setProposals] = useState(initialProposals);
  const [previousProposals, setPreviousProposals] = useState(initialProposals);
  const [loadingId, setLoadingId] = useState(null);
  // 채택된 제안이 이미 있으면(진행 중인 거래가 있으면) 바로 펼쳐서 보여주고,
  // 아직 비교/검토 단계면 목록을 접어둔 채 개수만 보여준다.
  const [expanded, setExpanded] = useState(() => initialProposals?.some((p) => p.isAdopted) ?? false);
  const router = useRouter();

  // 부모 컴포넌트에서 router.refresh()로 새로운 데이터가 내려올 때 상태 동기화
  if (previousProposals !== initialProposals) {
    setPreviousProposals(initialProposals);
    setProposals(initialProposals);
    if (initialProposals?.some((p) => p.isAdopted)) setExpanded(true);
  }

  if (!proposals || proposals.length === 0) {
    return (
      <div className="py-8 text-center text-sm text-slate-400">
        아직 등록된 수리 제안이 없습니다.
      </div>
    );
  }

  // 이미 채택된 제안이 하나라도 존재하는지 확인
  const hasAdopted = proposals.some((p) => p.isAdopted);

  if (!expanded) {
    return (
      <button
        type="button"
        onClick={() => setExpanded(true)}
        className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50/60 px-5 py-4 text-left transition hover:border-slate-300 hover:bg-slate-100"
      >
        <span className="text-sm font-bold text-slate-800">받은 제안 {proposals.length}개 보기</span>
        <CaretDown size={18} weight="bold" className="text-slate-400" />
      </button>
    );
  }

  // 채택된 제안(isAdopted === true)이 맨 위로 오도록 정렬
  const sortedProposals = [...proposals].sort((a, b) => {
    if (a.isAdopted === b.isAdopted) return 0;
    return a.isAdopted ? -1 : 1;
  });

  const handleAdopt = async (proposalId) => {
    if (!confirm("이 제안을 채택하시겠습니까?")) return;
    setLoadingId(proposalId);

    try {
      const response = await fetch(`/api/posts/${postId}/proposals/${proposalId}/adopt`, {
        method: "PATCH",
      });

      if (response.ok) {
        setProposals((current) => current.map((proposal) => proposal.id === proposalId ? { ...proposal, isAdopted: true } : proposal));
        alert("제안이 채택되었습니다.");
        router.refresh();
      } else {
        alert("제안 채택에 실패했습니다.");
      }
    } catch {
      alert("서버 연결에 실패했습니다.");
    } finally {
      setLoadingId(null);
    }
  };

  const handleDelete = async (proposalId) => {
    if (!confirm("정말 이 제안을 삭제하시겠습니까?")) return;
    setLoadingId(proposalId);

    try {
      const response = await fetch(`/api/posts/${postId}/proposals/${proposalId}`, {
        method: "DELETE",
      });

      if (response.ok) {
        alert("제안이 삭제되었습니다.");
        setProposals(proposals.filter((p) => p.id !== proposalId));
        router.refresh();
      } else {
        alert("제안 삭제에 실패했습니다.");
      }
    } catch {
      alert("서버 연결에 실패했습니다.");
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="space-y-4">
      <button
        type="button"
        onClick={() => setExpanded(false)}
        className="flex items-center gap-1 text-xs font-semibold text-slate-400 hover:text-slate-600"
      >
        <CaretUp size={14} weight="bold" />
        접기
      </button>
      {sortedProposals.map((proposal) => {
        const isMyProposal = userEmail && proposal.repairerEmail === userEmail;
        const isAdopted = proposal.isAdopted;

        return (
          <div
            key={proposal.id}
            className={`rounded-2xl border p-5 transition ${
              isAdopted
                ? "border-emerald-500 bg-emerald-50/40 shadow-sm"
                : "border-slate-100 bg-slate-50/50 hover:border-slate-200"
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold text-slate-800">
                  {proposal.repairerEmail || "수리공 이웃"}
                </span>
                {isAdopted && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-bold text-emerald-700">
                    <CheckCircle size={14} weight="bold" /> 채택 완료
                  </span>
                )}
              </div>
              <span className="text-xs text-slate-400">
                {proposal.createdAt ? new Date(proposal.createdAt).toLocaleDateString() : ""}
              </span>
            </div>

            {(proposal.repairerRegion || proposal.repairerCompletedCount > 0) && (
              <div className="mb-2 flex items-center gap-3 text-xs text-slate-500">
                {proposal.repairerRegion && (
                  <span className="inline-flex items-center gap-1">
                    <MapPin size={12} weight="duotone" /> {proposal.repairerRegion}
                  </span>
                )}
                {proposal.repairerCompletedCount > 0 && (
                  <span className="inline-flex items-center gap-1">
                    <Wrench size={12} weight="duotone" /> 완료한 수리 {proposal.repairerCompletedCount}건
                  </span>
                )}
              </div>
            )}

            {proposal.estimatedPrice !== undefined && proposal.estimatedPrice !== null && (
              <div className="mb-2 text-sm font-bold text-blue-600">
                희망 견적: {proposal.estimatedPrice.toLocaleString()}원
              </div>
            )}
            
            <p className="text-sm text-slate-700 whitespace-pre-line mb-4">{proposal.content}</p>
            
            <div className="flex items-center justify-end gap-2">
              {/* 채택 전이고, 이 제안의 당사자(글쓴이 또는 이 제안을 보낸 수리공)만 미리 채팅 가능 */}
              {!isAdopted && (isMine || isMyProposal) && (
                <ProposalChatRoom
                  key={`compact-${proposal.id}`}
                  proposalId={proposal.id}
                  isRequester={Boolean(isMine)}
                  isRepairer={Boolean(isMyProposal)}
                  compact
                />
              )}

              {/* 본인 제안이고 채택되지 않았을 때만 삭제 가능 */}
              {isMyProposal && !isAdopted && (
                <button
                  onClick={() => handleDelete(proposal.id)}
                  disabled={loadingId !== null}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-slate-200 px-4 py-2 text-xs font-semibold text-slate-700 shadow-sm hover:bg-slate-300 transition disabled:opacity-50"
                >
                  <Trash size={16} weight="bold" />
                  삭제
                </button>
              )}

              {/* 게시글 작성자이고, 아직 채택된 제안이 없으며, 본인 제안도 채택되지 않았을 때만 채택 버튼 노출 */}
              {isMine && !hasAdopted && !isAdopted && (
                <button
                  onClick={() => handleAdopt(proposal.id)}
                  disabled={loadingId !== null}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-emerald-700 transition disabled:opacity-50"
                >
                  <CheckCircle size={16} weight="bold" />
                  {loadingId === proposal.id ? "처리 중..." : "제안 채택하기"}
                </button>
              )}
            </div>
            {isAdopted && (isMine || isMyProposal) && (
              <>
                <ProposalChatRoom
                  key={`${proposal.id}-${proposal.fixDealId}`}
                  proposalId={proposal.id}
                  isRequester={Boolean(isMine)}
                  isRepairer={Boolean(isMyProposal)}
                />
                <FixDealProgress
                  fixDealId={proposal.fixDealId}
                  postId={postId}
                  isRequester={Boolean(isMine)}
                  isRepairer={Boolean(isMyProposal)}
                  estimatedPrice={proposal.estimatedPrice}
                  repairerEmail={proposal.repairerEmail}
                  userEmail={userEmail}
                />
              </>
            )}
          </div>
        );
      })}
    </div>
  );
}