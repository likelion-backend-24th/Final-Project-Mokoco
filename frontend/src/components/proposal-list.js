"use client";

import { useState } from "react";
import { CheckCircle, Trash } from "@phosphor-icons/react";
import { useRouter } from "next/navigation";

export default function ProposalList({ postId, proposals: initialProposals, isMine, userEmail }) {
  const [proposals, setProposals] = useState(initialProposals);
  const [loadingId, setLoadingId] = useState(null);
  const router = useRouter();

  if (!proposals || proposals.length === 0) {
    return (
      <div className="py-8 text-center text-sm text-slate-400">
        아직 등록된 수리 제안이 없습니다.
      </div>
    );
  }

  const handleAdopt = async (proposalId) => {
    if (!confirm("이 제안을 채택하시겠습니까?")) return;
    setLoadingId(proposalId);

    try {
      const response = await fetch(`/api/posts/${postId}/proposals/${proposalId}/adopt`, {
        method: "PATCH",
      });

      if (response.ok) {
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
      {proposals.map((proposal) => {
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
            <div className="flex items-center justify-between mb-2">
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
            
            <p className="text-sm text-slate-700 whitespace-pre-line mb-4">{proposal.content}</p>
            
            <div className="flex justify-end gap-2">
              {/* 본인 제안이고 채택되지 않았을 때만 삭제 가능 */}
              {isMyProposal && !isAdopted && (
                <button
                  onClick={() => handleDelete(proposal.id)}
                  disabled={loadingId === proposal.id}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-slate-200 px-4 py-2 text-xs font-semibold text-slate-700 shadow-sm hover:bg-slate-300 transition disabled:opacity-50"
                >
                  <Trash size={16} weight="bold" />
                  삭제
                </button>
              )}

              {/* 게시글 작성자이고 채택되지 않았을 때 채택 버튼 노출 */}
              {isMine && !isAdopted && (
                <button
                  onClick={() => handleAdopt(proposal.id)}
                  disabled={loadingId === proposal.id}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-emerald-700 transition disabled:opacity-50"
                >
                  <CheckCircle size={16} weight="bold" />
                  {loadingId === proposal.id ? "처리 중..." : "제안 채택하기"}
                </button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}