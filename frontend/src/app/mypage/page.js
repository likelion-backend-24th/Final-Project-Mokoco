"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import SiteHeader from "@/components/site-header";
import { useAuthStore } from "@/store/authStore";

const STATUS_LABEL = {
  COMPLETED: { text: "완료", className: "bg-green-50 text-green-700" },
  FAILED: { text: "실패", className: "bg-red-50 text-red-600" },
};

function formatDate(value) {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "-";
  return d.toLocaleString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function formatAmount(amount) {
  if (typeof amount !== "number") return "-";
  return `${amount.toLocaleString("ko-KR")}원`;
}

export default function MyPage() {
  const { userEmail } = useAuthStore();
  const [payments, setPayments] = useState([]);
  const [status, setStatus] = useState("loading"); // loading | ready | error
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    fetch("/api/payments/mine", { cache: "no-store" })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "정산내역을 불러오지 못했습니다.");
        return data;
      })
      .then((data) => {
        if (!active) return;
        setPayments(Array.isArray(data) ? data : []);
        setStatus("ready");
      })
      .catch((err) => {
        if (!active) return;
        setError(err.message);
        setStatus("error");
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <>
      <SiteHeader />
      <main className="mx-auto max-w-2xl px-4 py-8">
        <nav className="mb-4 text-sm text-blue-600">
          <Link href="/posts">← 수리 요청 목록</Link>
        </nav>

        <h1 className="text-2xl font-bold text-slate-900">내 프로필</h1>
        {userEmail && <p className="mt-1 text-sm text-slate-500">{userEmail}</p>}

        <section className="mt-8">
          <h2 className="text-lg font-semibold text-slate-800">정산내역</h2>
          <p className="mt-1 text-sm text-slate-500">
            내가 결제한 건과, 수리자로서 정산받은 건을 모두 최신순으로 보여드려요.
          </p>

          {status === "loading" && (
            <p className="mt-4 text-sm text-slate-400">불러오는 중...</p>
          )}

          {status === "error" && (
            <p role="alert" className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
              {error}
            </p>
          )}

          {status === "ready" && payments.length === 0 && (
            <p className="mt-4 rounded-xl border border-slate-200 bg-white px-4 py-6 text-center text-sm text-slate-400">
              아직 정산내역이 없어요.
            </p>
          )}

          {status === "ready" && payments.length > 0 && (
            <ul className="mt-4 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
              {payments.map((p) => {
                const isPayer = p.payerEmail === userEmail;
                const roleLabel = isPayer ? "결제함 (의뢰자)" : "정산받음 (수리자)";
                const displayAmount = isPayer ? p.amount : p.netAmount;
                const badge = STATUS_LABEL[p.status] ?? { text: p.status, className: "bg-slate-100 text-slate-600" };

                return (
                  <li key={p.id} className="flex items-center justify-between gap-4 px-4 py-4">
                    <div>
                      <p className="text-sm font-semibold text-slate-800">
                        수리 요청 #{p.postId} · {roleLabel}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-500">{formatDate(p.paidAt || p.createdAt)}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-slate-800">{formatAmount(displayAmount)}</span>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${badge.className}`}>
                        {badge.text}
                      </span>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </main>
    </>
  );
}
