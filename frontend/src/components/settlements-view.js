"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

const STATUS_LABEL = { COMPLETED: "결제 완료", FAILED: "결제 실패" };

function won(amount) {
  return `${(amount ?? 0).toLocaleString("ko-KR")}원`;
}

export default function SettlementsView() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    fetch("/api/payments/mine?size=50", { cache: "no-store" })
      .then(async (res) => {
        const payload = await res.json();
        if (!res.ok) throw new Error(payload.error || "정산 내역을 불러오지 못했습니다.");
        if (active) setData(payload);
      })
      .catch((err) => { if (active) setError(err.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <nav className="mb-4 text-sm text-blue-600">
        <Link href="/posts">← 수리 요청 목록</Link>
      </nav>
      <h1 className="text-2xl font-bold text-slate-900">정산 내역</h1>
      <p className="mt-1 text-sm text-slate-500">
        내가 수리자로서 받은 결제 목록입니다. 거래가 완료되면 정산이 확정되고, 실제 계좌 지급은 운영팀이 별도로 처리합니다.
      </p>

      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>}
      {loading && <p className="mt-6 text-sm text-slate-400">불러오는 중...</p>}

      {data && (
        <>
          <div className="mt-6 grid grid-cols-2 gap-3">
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3">
              <p className="text-xs font-semibold text-emerald-700">정산 확정</p>
              <p className="mt-1 text-lg font-extrabold text-emerald-700">{won(data.settledAmount)}</p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
              <p className="text-xs font-semibold text-slate-500">정산 대기</p>
              <p className="mt-1 text-lg font-extrabold text-slate-700">{won(data.pendingAmount)}</p>
            </div>
          </div>

          {data.payments.length === 0 ? (
            <p className="mt-8 text-center text-sm text-slate-400">아직 받은 결제가 없습니다.</p>
          ) : (
            <ul className="mt-6 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
              {data.payments.map((payment) => (
                <li key={payment.id} className="px-4 py-4">
                  <div className="flex items-center justify-between gap-3">
                    <Link href={`/posts/${payment.postId}`} className="text-sm font-semibold text-slate-800 hover:underline">
                      게시글 #{payment.postId}
                    </Link>
                    <span className="text-lg font-extrabold text-slate-900">{won(payment.netAmount)}</span>
                  </div>
                  <div className="mt-1 flex items-center gap-2 text-xs text-slate-500">
                    <span>{STATUS_LABEL[payment.status] ?? payment.status}</span>
                    <span aria-hidden>·</span>
                    {payment.settledAt ? (
                      <span className="font-semibold text-emerald-600">정산 완료 ({new Date(payment.settledAt).toLocaleDateString("ko-KR")})</span>
                    ) : (
                      <span>거래 완료 시 정산 예정</span>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </main>
  );
}
