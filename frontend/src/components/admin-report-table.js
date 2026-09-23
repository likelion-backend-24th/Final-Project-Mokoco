"use client";

import { useState } from "react";
import Link from "next/link";
import { CheckCircle, XCircle, Trash, Prohibit } from "@phosphor-icons/react";

const REASON_LABEL = { SPAM: "스팸/광고", ABUSE: "욕설/괴롭힘", SCAM: "사기/노쇼", INAPPROPRIATE: "부적절한 내용", OTHER: "기타" };
const STATUS_STYLE = {
  PENDING: "bg-amber-100 text-amber-700",
  RESOLVED: "bg-emerald-100 text-emerald-700",
  DISMISSED: "bg-slate-100 text-slate-500",
};
const STATUS_LABEL = { PENDING: "대기 중", RESOLVED: "조치 완료", DISMISSED: "기각" };
const FILTERS = [
  { value: "ALL", label: "전체" },
  { value: "POST", label: "글" },
  { value: "USER", label: "유저" },
];

export default function AdminReportTable({ initialReports }) {
  const [reports, setReports] = useState(initialReports);
  const [filter, setFilter] = useState("ALL");
  const [loadingId, setLoadingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function act(report, action) {
    const response = await fetch(`/api/admin/reports/${report.id}/${action}`, { method: "PATCH" });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || "처리에 실패했습니다.");
    setReports((current) => current.map((r) => (r.id === report.id ? data : r)));
  }

  async function handleReview(report, action) {
    setLoadingId(report.id);
    setError("");
    setNotice("");
    try {
      await act(report, action);
      setNotice(action === "resolve" ? "신고를 조치 완료로 표시했어요." : "신고를 기각했어요.");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoadingId(null);
    }
  }

  async function handleDeletePost(report) {
    if (!window.confirm(`글 #${report.targetId}을(를) 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`)) return;
    setLoadingId(report.id);
    setError("");
    setNotice("");
    try {
      const response = await fetch(`/api/admin/posts/${report.targetId}`, { method: "DELETE" });
      if (!response.ok && response.status !== 204) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error || "글 삭제에 실패했습니다.");
      }
      await act(report, "resolve");
      setNotice("글을 삭제하고 신고를 조치 완료로 표시했어요.");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoadingId(null);
    }
  }

  async function handleSuspendUser(report) {
    if (!report.targetEmail) return;
    if (!window.confirm(`${report.targetEmail} 계정을 정지하시겠습니까?`)) return;
    setLoadingId(report.id);
    setError("");
    setNotice("");
    try {
      const response = await fetch("/api/admin/users/by-email/status", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: report.targetEmail, status: "SUSPENDED" }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.error || "정지에 실패했습니다.");
      await act(report, "resolve");
      setNotice("계정을 정지하고 신고를 조치 완료로 표시했어요.");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoadingId(null);
    }
  }

  const visibleReports = filter === "ALL" ? reports : reports.filter((report) => report.targetType === filter);

  return (
    <div className="dashboard-card">
      <div className="mb-4 flex gap-2">
        {FILTERS.map(({ value, label }) => (
          <button
            key={value}
            type="button"
            onClick={() => setFilter(value)}
            className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition-colors ${
              filter === value ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-500 hover:bg-slate-200"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {notice && <p role="status" className="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{notice}</p>}
      {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>}

      {visibleReports.length === 0 ? (
        <p className="py-10 text-center text-sm text-slate-400">접수된 신고가 없어요.</p>
      ) : (
        <div className="space-y-3">
          {visibleReports.map((report) => (
            <div key={report.id} className="rounded-xl border border-slate-200 p-4">
              <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-bold text-slate-600">
                    {report.targetType === "POST" ? "글" : "유저"}
                  </span>
                  <span className="text-sm font-bold text-slate-800">
                    {report.targetType === "POST" ? (
                      <Link href={`/posts/${report.targetId}`} className="text-blue-600 hover:underline">글 #{report.targetId} 보기</Link>
                    ) : (
                      report.targetEmail || "이메일 정보 없음"
                    )}
                  </span>
                  <span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${STATUS_STYLE[report.status]}`}>
                    {STATUS_LABEL[report.status]}
                  </span>
                </div>
                <span className="text-xs text-slate-400">{report.createdAt ? new Date(report.createdAt).toLocaleString() : ""}</span>
              </div>

              <p className="mb-1 text-sm text-slate-700">
                <span className="font-semibold">{REASON_LABEL[report.reason] ?? report.reason}</span>
                {report.detail && <span className="text-slate-500"> — {report.detail}</span>}
              </p>
              <p className="mb-3 text-xs text-slate-400">신고자: {report.reporterEmail}</p>

              {report.status === "PENDING" && (
                <div className="flex flex-wrap justify-end gap-2">
                  {report.targetType === "POST" && (
                    <button
                      type="button"
                      onClick={() => handleDeletePost(report)}
                      disabled={loadingId !== null}
                      className="inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-red-700 disabled:opacity-40"
                    >
                      <Trash size={14} weight="bold" />
                      글 삭제
                    </button>
                  )}
                  {report.targetType === "USER" && (
                    <button
                      type="button"
                      onClick={() => handleSuspendUser(report)}
                      disabled={loadingId !== null || !report.targetEmail}
                      className="inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-red-700 disabled:opacity-40"
                    >
                      <Prohibit size={14} weight="bold" />
                      계정 정지
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => handleReview(report, "dismiss")}
                    disabled={loadingId !== null}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-300 disabled:opacity-40"
                  >
                    <XCircle size={14} weight="bold" />
                    기각
                  </button>
                  <button
                    type="button"
                    onClick={() => handleReview(report, "resolve")}
                    disabled={loadingId !== null}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-40"
                  >
                    <CheckCircle size={14} weight="bold" />
                    {loadingId === report.id ? "처리 중..." : "조치 완료로 표시"}
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
