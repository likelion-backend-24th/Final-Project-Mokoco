"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Star, X } from "@phosphor-icons/react";

const STATUS_LABEL = {
  MATCHED: "매칭 완료",
  PRODUCT_SENT: "제품 전달 완료",
  REPAIRING: "수리 진행중",
  REPAIR_DONE: "수리완료 신청됨",
  COMPLETED: "거래 완료",
  CANCELED: "거래 취소됨",
};

export default function RepairerTransactionsModal({ userId, onClose }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/profile/${encodeURIComponent(userId)}/transactions?role=repairer&size=20`, {
      signal: controller.signal,
      cache: "no-store",
    })
      .then(async (res) => {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error ?? "거래 내역을 불러오지 못했습니다.");
        setData(json);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [userId]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose} role="presentation">
      <div
        className="max-h-[80vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-slate-100 p-4">
          <div>
            <h3 className="text-sm font-bold text-slate-800">거래 내역</h3>
            <p className="mt-0.5 text-xs text-slate-400">{userId}</p>
          </div>
          <button type="button" onClick={onClose} aria-label="닫기" className="rounded-full p-1 text-slate-400 hover:bg-slate-100">
            <X size={18} />
          </button>
        </div>

        <div className="p-4">
          {loading && <p className="text-sm text-slate-400">불러오는 중...</p>}
          {error && <p className="text-sm text-red-600">{error}</p>}

          {data && data.items.length === 0 && (
            <p className="py-6 text-center text-sm text-slate-400">아직 거래 내역이 없어요.</p>
          )}

          {data && data.items.length > 0 && (
            <ul className="space-y-2">
              {data.items.map((item) => (
                <li key={item.fixDealId} className="rounded-lg border border-slate-100 p-3">
                  <div className="flex items-center justify-between">
                    <Link href={`/posts/${item.postId}`} className="text-sm font-semibold text-slate-800 hover:underline">
                      {item.postTitle}
                    </Link>
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
                      {STATUS_LABEL[item.status] ?? item.status}
                    </span>
                  </div>
                  {item.review && (
                    <div className="mt-1.5 flex items-center gap-1 text-xs text-slate-500">
                      <Star size={12} weight="fill" className="text-amber-400" />
                      {item.review.rating}
                      <span className="line-clamp-1">{item.review.content}</span>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
