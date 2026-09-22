"use client";

import { useState } from "react";
import Link from "next/link";
import { CheckCircle, XCircle } from "@phosphor-icons/react";

const REASON_LABEL = { SPAM: "스팸/광고", ABUSE: "욕설/괴롭힘", SCAM: "사기/노쇼", INAPPROPRIATE: "부적절한 내용", OTHER: "기타" };
const STATUS_STYLE = {
  PENDING: "bg-amber-100 text-amber-700",
  RESOLVED: "bg-emerald-100 text-emerald-700",
  DISMISSED: "bg-slate-100 text-slate-500",
};
const STATUS_LABEL = { PENDING: "대기 중", RESOLVED: "조치 완료", DISMISSED: "기각" };

export default function AdminReportTable({ initialReports }) {
  const [reports, setReports] = useState(initialReports);
  const [loadingId, setLoadingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function act(report, action) {
    setLoadingId(report.id);
    setError("");
    setNotice("");
    try {
      const response = await fetch(`/api/admin/reports/${report.id}/${action}`, { method: "PATCH" });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "처리에 실패했습니다.");
      setReports((current) => current.map((r) => (r.id === report.id ? data : r)));
      setNotice(action === "resolve" ? "신고를 조치 완료로 표시했어요." : "신고를 기각했어요.");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoadingId(null);
    }
  }

  if (!reports || reports.length === 0) {
    return <div className="dashboard-card text-center text-sm text-slate-400 py-10">접수된 신고가 없어요.</div>;
  }

  return (
    <div className="dashboard-card">
      {notice && <p role="status" className="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{notice}</p>}
      {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>}
      <div className="space-y-3">
        {reports.map((report) => (
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
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => act(report, "dismiss")}
                  disabled={loadingId !== null}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-300 disabled:opacity-40"
                >
                  <XCircle size={14} weight="bold" />
                  기각
                </button>
                <button
                  type="button"
                  onClick={() => act(report, "resolve")}
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
      <p className="mt-4 text-xs text-slate-400">
        * 글 삭제는 글 상세 페이지의 &ldquo;관리자 삭제&rdquo;, 유저 정지는 위 회원 목록의 &ldquo;정지&rdquo; 버튼으로 실제 조치를 진행한 뒤 여기서 &ldquo;조치 완료&rdquo;로 표시해주세요.
      </p>
    </div>
  );
}
