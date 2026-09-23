"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { CaretLeft, CaretRight } from "@phosphor-icons/react";

const STATUS_OPTIONS = [
  { value: "ALL", label: "전체" },
  { value: "COMPLETED", label: "결제 완료" },
  { value: "FAILED", label: "결제 실패" },
  { value: "CANCELLED", label: "결제 취소" },
];
const STATUS_LABEL = { COMPLETED: "결제 완료", FAILED: "결제 실패", CANCELLED: "결제 취소" };
const STATUS_STYLE = {
  COMPLETED: "bg-emerald-100 text-emerald-700",
  FAILED: "bg-red-100 text-red-700",
  CANCELLED: "bg-slate-100 text-slate-500",
};

function won(amount) {
  return amount != null ? `${amount.toLocaleString("ko-KR")}원` : "—";
}

export default function AdminPaymentTable({ initialPage }) {
  const [status, setStatus] = useState("ALL");
  const [pageData, setPageData] = useState(initialPage);
  const [page, setPage] = useState(initialPage?.number ?? 0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const isFirstRun = useRef(true);

  useEffect(() => {
    if (isFirstRun.current) {
      isFirstRun.current = false;
      return;
    }

    const controller = new AbortController();
    const query = new URLSearchParams({ page: String(page), size: "20" });
    if (status) query.set("status", status);

    fetch(`/api/admin/payments?${query.toString()}`, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error ?? "결제 목록을 불러오지 못했습니다.");
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

  const payments = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 1;

  return (
    <div className="dashboard-card">
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

      {payments.length === 0 ? (
        <div className="py-10 text-center text-sm text-slate-400">
          {loading ? "불러오는 중..." : "해당하는 결제가 없어요."}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full table-fixed text-left text-sm">
            <colgroup>
              <col className="w-[10%]" />
              <col className="w-[18%]" />
              <col className="w-[18%]" />
              <col className="w-[12%]" />
              <col className="w-[13%]" />
              <col className="w-[15%]" />
              <col className="w-[14%]" />
            </colgroup>
            <thead>
              <tr className="border-b border-slate-200 text-xs text-slate-400">
                <th className="py-2 pr-3 font-semibold">글</th>
                <th className="py-2 pr-3 font-semibold">결제자</th>
                <th className="py-2 pr-3 font-semibold">수신자</th>
                <th className="py-2 pr-3 font-semibold">결제액</th>
                <th className="py-2 pr-3 font-semibold">상태</th>
                <th className="py-2 pr-3 font-semibold">결제일</th>
                <th className="py-2 pr-3 font-semibold">정산</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr key={payment.id} className="border-b border-slate-100 last:border-0">
                  <td className="py-2.5 pr-3">
                    <Link href={`/posts/${payment.postId}`} className="font-semibold text-blue-600 hover:underline">
                      글 #{payment.postId}
                    </Link>
                  </td>
                  <td className="truncate py-2.5 pr-3 text-slate-500" title={payment.payerNickname ?? payment.payerEmail ?? undefined}>
                    {payment.payerNickname ?? payment.payerEmail ?? "—"}
                  </td>
                  <td className="truncate py-2.5 pr-3 text-slate-500" title={payment.payeeNickname ?? payment.payeeEmail ?? undefined}>
                    {payment.payeeNickname ?? payment.payeeEmail ?? "—"}
                  </td>
                  <td className="py-2.5 pr-3 text-slate-600">{won(payment.amount)}</td>
                  <td className="whitespace-nowrap py-2.5 pr-3">
                    <span className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-bold ${STATUS_STYLE[payment.status] ?? "bg-slate-100 text-slate-500"}`}>
                      {STATUS_LABEL[payment.status] ?? payment.status}
                    </span>
                  </td>
                  <td className="py-2.5 pr-3 text-xs text-slate-400">
                    {payment.paidAt ? new Date(payment.paidAt).toLocaleString("ko-KR") : "—"}
                  </td>
                  <td className="py-2.5 pr-3 text-xs text-slate-400">
                    {payment.settledAt ? new Date(payment.settledAt).toLocaleDateString("ko-KR") : "—"}
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
