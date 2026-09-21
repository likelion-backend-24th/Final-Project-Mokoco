"use client";

import { useState, useEffect } from "react";
import { CheckCircle, Trash } from "@phosphor-icons/react";
import { useRouter } from "next/navigation";
import { XCircle } from 'lucide-react';
import ProposalChatRoom from "@/components/proposal-chat-room";
import FixDealProgress from "@/components/fix-deal-progress";
import RatingBadge from "@/components/rating-badge";
import RepairerMenu from "@/components/repairer-menu";

export default function ProposalList({ postId, proposals: initialProposals, isMine, userEmail, userId }) {
  const [proposals, setProposals] = useState(initialProposals);
  const [previousProposals, setPreviousProposals] = useState(initialProposals);
  const [loadingId, setLoadingId] = useState(null);
  const [adoptedDealStatus, setAdoptedDealStatus] = useState(null);
  const router = useRouter();

  const adoptedFixDealId = proposals?.find((p) => p.isAdopted)?.fixDealId ?? null;

  useEffect(() => {
    if (!adoptedFixDealId) return;
    const controller = new AbortController();
    fetch(`/api/fix-deals/${adoptedFixDealId}`, { signal: controller.signal, cache: "no-store" })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (!controller.signal.aborted) setAdoptedDealStatus(data?.status ?? null);
      })
      .catch(() => {});
    return () => controller.abort();
  }, [adoptedFixDealId]);

  // 부모 컴포넌트에서 router.refresh()로 새로운 데이터가 내려올 때 상태 동기화
  if (previousProposals !== initialProposals) {
    setPreviousProposals(initialProposals);
    setProposals(initialProposals);
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


  const handleCancelAdopt = async (proposalId) => {
    if (!confirm("이 제안 채택을 취소하시겠습니까?")) return;

    setLoadingId(proposalId);

    try {
      const response = await fetch(
        `/api/posts/${postId}/proposals/${proposalId}/cancel`,
        {
          method: "PATCH",
          credentials: "include",
        }
      );
      if (response.ok) {
        setProposals((current) =>
          current.map((proposal) =>
            proposal.id === proposalId
              ? { ...proposal, isAdopted: false }
              : proposal
          )
        );

        alert("제안 채택이 취소되었습니다.");
        router.refresh();
      } else {
        console.error("채택 취소 실패:", response.status);
        alert("제안 채택 취소에 실패했습니다.");
      }
    } catch (error) {
      console.error(error);
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
        const payload = await response.json().catch(() => ({}));
        alert(payload.error || payload.message || "채팅방이 있거나 채택된 견적은 삭제할 수 없습니다.");
      }
    } catch {
      alert("서버 연결에 실패했습니다.");
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="space-y-4">
      {sortedProposals.map((proposal) => {
        const isMyProposal = userId && String(proposal.repairerId) === String(userId);
        const isAdopted = proposal.isAdopted;

        return (
          <div
            key={proposal.id}
            className={`rounded-2xl border p-5 transition ${isAdopted
              ? "border-emerald-500 bg-emerald-50/40 shadow-sm"
              : "border-slate-100 bg-slate-50/50 hover:border-slate-200"
              }`}
          >
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <RepairerMenu userId={proposal.repairerId} nickname={proposal.repairerNickname} />
                <RatingBadge userId={proposal.repairerId} postId={postId} />
                {isAdopted && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-bold text-emerald-700">
                    <CheckCircle size={14} weight="bold" />
                    {adoptedDealStatus === "COMPLETED" ? "거래 완료" : "채택 완료"}
                  </span>
                )}
              </div>
              <span className="text-xs text-slate-400">
                {proposal.createdAt ? new Date(proposal.createdAt).toLocaleDateString() : ""}
              </span>
            </div>

            {proposal.estimatedPrice !== undefined && proposal.estimatedPrice !== null && (
              <div className="mb-2 text-sm font-bold text-blue-600">
                희망 견적: {proposal.estimatedPrice.toLocaleString()}원
              </div>
            )}

            <p className="text-sm text-slate-700 whitespace-pre-line mb-4">{proposal.content}</p>

            <div className="flex justify-end gap-2">
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

              {/* 게시글 작성자이고, 현재 해당 제안이 채택된 상태(MATCHED, 결제 전)일 때만 채택 취소 버튼 */}
              {isMine && isAdopted && adoptedDealStatus === "MATCHED" && (
                <button
                  onClick={() => handleCancelAdopt(proposal.id)}
                  disabled={loadingId !== null}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-red-500 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-red-600 transition disabled:opacity-50"
                >
                  <XCircle size={16} weight="bold" />
                  {loadingId === proposal.id ? "처리 중..." : "채택 취소"}
                </button>
              )}

            </div>
            {(isMine || isMyProposal) && <ProposalChatRoom key={proposal.id} proposalId={proposal.id} />}
            {isAdopted && (isMine || isMyProposal) && (
              <>
                <FixDealProgress
                  fixDealId={proposal.fixDealId}
                  postId={postId}
                  isRequester={Boolean(isMine)}
                  isRepairer={Boolean(isMyProposal)}
                  estimatedPrice={proposal.estimatedPrice}
                  repairerId={proposal.repairerId}
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