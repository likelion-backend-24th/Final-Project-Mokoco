"use client";

import { useState } from "react";
import { Flag } from "@phosphor-icons/react";

const REASONS = [
  { value: "SPAM", label: "스팸/광고" },
  { value: "ABUSE", label: "욕설/괴롭힘" },
  { value: "SCAM", label: "사기/노쇼" },
  { value: "INAPPROPRIATE", label: "부적절한 내용" },
  { value: "OTHER", label: "기타" },
];

// 글(POST)과 작성자(USER)를 하나의 버튼에서 선택해 신고할 수 있게 한다.
// postId/authorEmail 둘 다 있으면 "이 글" / "작성자" 두 대상 중 선택, authorEmail만 있으면 유저 신고 전용으로 동작.
export default function ReportButton({ postId, authorEmail, label = "신고하기", defaultOpen = false, onClose }) {
  const [open, setOpen] = useState(defaultOpen);
  const [target, setTarget] = useState(postId ? "POST" : "USER");
  const [reason, setReason] = useState("SPAM");
  const [detail, setDetail] = useState("");
  const [status, setStatus] = useState("idle"); // idle | submitting | done | error
  const [message, setMessage] = useState("");

  async function submit(event) {
    event.preventDefault();
    setStatus("submitting");
    setMessage("");
    try {
      const response = await fetch("/api/reports", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          targetType: target,
          targetId: target === "POST" ? postId : null,
          targetEmail: authorEmail ?? null,
          reason,
          detail: detail.trim() || null,
        }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "신고 접수에 실패했습니다.");
      setStatus("done");
    } catch (failure) {
      setStatus("error");
      setMessage(failure.message);
    }
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-500 hover:border-red-200 hover:text-red-500 transition"
      >
        <Flag size={14} weight="bold" />
        {label}
      </button>
    );
  }

  if (status === "done") {
    return <p className="rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-600">신고가 접수됐어요. 검토 후 조치할게요.</p>;
  }

  return (
    <form onSubmit={submit} className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <span className="text-sm font-bold text-slate-800">신고하기</span>
        <button
          type="button"
          onClick={() => { setOpen(false); onClose?.(); }}
          className="text-xs text-slate-400 hover:text-slate-600"
        >
          닫기
        </button>
      </div>

      {postId && authorEmail && (
        <div className="mb-3 flex gap-2">
          <button type="button" onClick={() => setTarget("POST")}
            className={`flex-1 rounded-lg px-2 py-1.5 text-xs font-semibold ${target === "POST" ? "bg-red-50 text-red-600 border border-red-200" : "bg-slate-100 text-slate-500"}`}>
            이 글 신고
          </button>
          <button type="button" onClick={() => setTarget("USER")}
            className={`flex-1 rounded-lg px-2 py-1.5 text-xs font-semibold ${target === "USER" ? "bg-red-50 text-red-600 border border-red-200" : "bg-slate-100 text-slate-500"}`}>
            작성자 신고
          </button>
        </div>
      )}

      <label className="mb-1 block text-xs font-semibold text-slate-500">사유</label>
      <select value={reason} onChange={(e) => setReason(e.target.value)} className="mb-3 w-full rounded-lg border border-slate-200 px-2 py-1.5 text-sm">
        {REASONS.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
      </select>

      <label className="mb-1 block text-xs font-semibold text-slate-500">상세 내용 (선택)</label>
      <textarea value={detail} onChange={(e) => setDetail(e.target.value)} rows={3} maxLength={1000}
        className="mb-3 w-full rounded-lg border border-slate-200 px-2 py-1.5 text-sm" placeholder="상황을 알려주시면 검토에 도움이 돼요." />

      {message && <p role="alert" className="mb-2 text-xs text-red-600">{message}</p>}

      <button type="submit" disabled={status === "submitting"}
        className="w-full rounded-lg bg-red-500 px-3 py-2 text-sm font-semibold text-white hover:bg-red-600 disabled:opacity-50">
        {status === "submitting" ? "접수 중..." : "신고 제출"}
      </button>
    </form>
  );
}
