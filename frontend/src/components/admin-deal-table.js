"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { CaretLeft, CaretRight } from "@phosphor-icons/react";

const STATUS_OPTIONS = [
  { value: "ALL", label: "전체" },
  { value: "", label: "진행 중" },
  { value: "MATCHED", label: "작업 전" },
  { value: "REPAIRING", label: "수리 진행 중" },
  { value: "REPAIR_DONE", label: "완료 확인 대기" },
  { value: "COMPLETED", label: "거래 완료" },
  { value: "CANCELED", label: "거래 취소" },
];

function won(amount) {
  return `${(amount ?? 0).toLocaleString("ko-KR")}원`;
}

const STATUS_LABEL = {
  MATCHED: "작업 전",
  REPAIRING: "수리 진행 중",
  REPAIR_DONE: "완료 확인 대기",
  COMPLETED: "거래 완료",
  CANCELED: "거래 취소",
  PRODUCT_SENT: "물품 전달",
};
const STATUS_STYLE = {
  MATCHED: "bg-blue-100 text-blue-700",
  REPAIRING: "bg-amber-100 text-amber-700",
  REPAIR_DONE: "bg-purple-100 text-purple-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  CANCELED: "bg-slate-100 text-slate-500",
  PRODUCT_SENT: "bg-slate-100 text-slate-500",
};

export default function AdminDealTable({ initialPage }) {
  const [status, setStatus] = useState("ALL");
  const [pageData, setPageData] = useState(initialPage);
  const [page, setPage] = useState(initialPage?.number ?? 0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [summary, setSummary] = useState(null);
  // 최초 마운트 시 서버가 이미 내려준 initialPage(진행중/0페이지)를 그대로 쓰고,
  // 필터나 페이지가 바뀔 때만 다시 불러온다.
  const isFirstRun = useRef(true);

  useEffect(() => {
    if (isFirstRun.current) {
      isFirstRun.current = false;
      return;
    }

    const controller = new AbortController();
    const query = new URLSearchParams({ page: String(page), size: "20" });
    if (status) query.set("status", status);

    fetch(`/api/admin/deals?${query.toString()}`, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error ?? "거래 목록을 불러오지 못했습니다.");
        setPageData(json);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [status, page]);

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/admin/deals/summary", { signal: controller.signal, cache: "no-store" })
      .then((res) => (res.ok ? res.json() : null))
      .then((json) => { if (json) setSummary(json); })
      .catch(() => {});
    return () => controller.abort();
  }, []);

  function changeStatus(value) {
    setStatus(value);
    setPage(0);
    setLoading(true);
    setError("");
  }

  function goToPage(next) {
    setPage(next);
    setLoading(true);
    setError("");
  }

  const deals = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 1;

  return (
    <div className="dashboard-card">
      {summary && (
        <div className="mb-4 grid grid-cols-2 gap-3">
          <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3">
            <p className="text-xs font-semibold text-emerald-700">거래 완료 금액</p>
            <p className="mt-1 text-lg font-extrabold text-emerald-700">{won(summary.totalCompletedAmount)}</p>
          </div>
          <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs font-semibold text-slate-500">정산된 금액</p>
            <p className="mt-1 text-lg font-extrabold text-slate-700">{won(summary.totalSettledAmount)}</p>
          </div>
        </div>
      )}

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <select
          value={status}
          onChange={(event) => changeStatus(event.target.value)}
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-600 outline-none focus:border-blue-400"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
        <span className="text-xs text-slate-400">
          {pageData ? `총 ${pageData.totalElements.toLocaleString("ko-KR")}건` : ""}
        </span>
      </div>

      {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>}

      {deals.length === 0 ? (
        <div className="py-10 text-center text-sm text-slate-400">
          {loading ? "불러오는 중..." : "해당하는 거래가 없어요."}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs text-slate-400">
                <th className="py-2 pr-3 font-semibold">글</th>
                <th className="py-2 pr-3 font-semibold">상태</th>
                <th className="py-2 pr-3 font-semibold">견적가</th>
                <th className="py-2 pr-3 font-semibold">의뢰인</th>
                <th className="py-2 pr-3 font-semibold">수리자</th>
                <th className="py-2 pr-3 font-semibold">생성일</th>
              </tr>
            </thead>
            <tbody>
              {deals.map((deal) => (
                <tr key={deal.id} className="border-b border-slate-100 last:border-0">
                  <td className="py-2.5 pr-3">
                    <Link href={`/posts/${deal.postId}`} className="font-semibold text-blue-600 hover:underline">
                      {deal.postTitle ?? `글 #${deal.postId}`}
                    </Link>
                  </td>
                  <td className="whitespace-nowrap py-2.5 pr-3">
                    <span className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-bold ${STATUS_STYLE[deal.status] ?? "bg-slate-100 text-slate-500"}`}>
                      {STATUS_LABEL[deal.status] ?? deal.status}
                    </span>
                  </td>
                  <td className="py-2.5 pr-3 text-slate-600">
                    {deal.estimatedPrice != null ? `${deal.estimatedPrice.toLocaleString("ko-KR")}원` : "—"}
                  </td>
                  <td className="py-2.5 pr-3 text-slate-500">{deal.requesterEmail ?? `#${deal.requesterId}`}</td>
                  <td className="py-2.5 pr-3 text-slate-500">{deal.repairerEmail ?? `#${deal.repairerId}`}</td>
                  <td className="py-2.5 pr-3 text-xs text-slate-400">
                    {deal.createdAt ? new Date(deal.createdAt).toLocaleString("ko-KR") : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-3">
          <button
            type="button"
            onClick={() => goToPage(Math.max(page - 1, 0))}
            disabled={page === 0 || loading}
            className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50 disabled:opacity-40"
          >
            <CaretLeft size={14} weight="bold" />
            이전
          </button>
          <span className="text-xs text-slate-400">{page + 1} / {totalPages}</span>
          <button
            type="button"
            onClick={() => goToPage(Math.min(page + 1, totalPages - 1))}
            disabled={page >= totalPages - 1 || loading}
            className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50 disabled:opacity-40"
          >
            다음
            <CaretRight size={14} weight="bold" />
          </button>
        </div>
      )}
    </div>
  );
}
