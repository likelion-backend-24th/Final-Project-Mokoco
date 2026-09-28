"use client";

import { useEffect, useState } from "react";
import { DEAL_STATUS_LABEL } from "@/lib/deal-status-label";

// 결제/진행상태 조작은 전부 채팅방의 계약서 페이지(repair-contract.js)에서 이루어진다.
// 여기서는 지금 어느 단계인지만 읽기 전용으로 보여준다.
export default function FixDealProgress({ fixDealId }) {
  const [status, setStatus] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!fixDealId) return;
    const controller = new AbortController();
    fetch(`/api/fix-deals/${fixDealId}`, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) throw new Error(data.error ?? "거래 상태를 불러오지 못했습니다.");
        setStatus(data.status);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      });
    return () => controller.abort();
  }, [fixDealId]);

  if (!fixDealId) return null;
  if (error) return <p role="alert" className="mt-4 text-sm text-red-600">거래 상태를 불러오지 못했어요: {error}</p>;
  if (!status) return null;

  return (
    <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50/60 p-4">
      <span className="text-sm font-bold text-slate-800">거래 진행 상태: {DEAL_STATUS_LABEL[status] ?? status}</span>
    </div>
  );
}
