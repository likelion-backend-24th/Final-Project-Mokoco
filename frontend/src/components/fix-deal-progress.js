"use client";

import { useEffect, useState } from "react";
import { CheckCircle, CreditCard, Wrench } from "@phosphor-icons/react";

const STATUS_LABEL = {
  MATCHED: "매칭 완료",
  PRODUCT_SENT: "제품 전달 완료",
  REPAIRING: "수리 진행중",
  REPAIR_DONE: "수리완료 신청됨",
  COMPLETED: "거래 완료",
  CANCELED: "거래 취소됨",
};

export default function FixDealProgress({ fixDealId, postId, isRequester, isRepairer, estimatedPrice, repairerEmail }) {
  const [deal, setDeal] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [refreshToken, setRefreshToken] = useState(0);

  function refresh() {
    setRefreshToken((token) => token + 1);
  }

  useEffect(() => {
    if (!fixDealId) return;
    const controller = new AbortController();

    fetch(`/api/fix-deals/${fixDealId}`, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) throw new Error(data.error ?? "거래 상태를 불러오지 못했습니다.");
        setDeal(data);
        setError("");

        if (data.status === "REPAIR_DONE" || data.status === "COMPLETED") {
          const payRes = await fetch(`/api/payments/post/${postId}`, { signal: controller.signal, cache: "no-store" });
          setPayment(payRes.ok ? await payRes.json() : null);
        } else {
          setPayment(null);
        }
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [fixDealId, postId, refreshToken]);

  async function startRepair() {
    setActionLoading(true);
    setError("");
    try {
      const sendRes = await fetch(`/api/fix-deals/${fixDealId}/product-sent`, { method: "PATCH" });
      const sendData = await sendRes.json().catch(() => ({}));
      if (!sendRes.ok) {
        setError(sendData.error ?? "거래 시작 처리에 실패했습니다.");
        return;
      }

      const repairRes = await fetch(`/api/fix-deals/${fixDealId}/repairing`, { method: "PATCH" });
      const repairData = await repairRes.json().catch(() => ({}));
      if (!repairRes.ok) {
        // product-sent는 이미 반영됐으니, 상태를 다시 불러와 "수리 시작" 버튼이 이어서 뜨도록 한다.
        setError(repairData.error ?? "수리 시작 처리에 실패했습니다.");
        refresh();
        return;
      }

      refresh();
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setActionLoading(false);
    }
  }

  async function runAction(url, method, body) {
    setActionLoading(true);
    setError("");
    try {
      const res = await fetch(url, {
        method,
        headers: body ? { "Content-Type": "application/json" } : undefined,
        body: body ? JSON.stringify(body) : undefined,
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error ?? "처리하지 못했습니다.");
        return;
      }
      refresh();
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setActionLoading(false);
    }
  }

  if (!fixDealId) return null;
  if (loading) return <p className="mt-4 text-sm text-slate-400">거래 상태 확인 중...</p>;
  if (error && !deal) {
    return (
      <p role="alert" className="mt-4 text-sm text-red-600">
        거래 상태를 불러오지 못했어요: {error}
      </p>
    );
  }
  if (!deal) return null;

  const status = deal.status;
  const paid = payment?.status === "COMPLETED";

  return (
    <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50/60 p-4">
      <span className="text-sm font-bold text-slate-800">
        거래 진행 상태: {STATUS_LABEL[status] ?? status}
      </span>

      {error && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {error}
        </p>
      )}

      <div className="mt-3 flex flex-wrap items-center gap-2">
        {status === "MATCHED" && isRepairer && (
          <button
            type="button"
            onClick={startRepair}
            disabled={actionLoading}
            className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            <Wrench size={16} weight="bold" />
            {actionLoading ? "처리 중..." : "수리 시작"}
          </button>
        )}
        {status === "MATCHED" && isRequester && (
          <p className="text-xs text-slate-500">수리자에게 제품을 전달해주세요. 수리자가 확인하면 다음 단계로 넘어가요.</p>
        )}

        {status === "PRODUCT_SENT" && isRepairer && (
          <button
            type="button"
            onClick={() => runAction(`/api/fix-deals/${fixDealId}/repairing`, "PATCH")}
            disabled={actionLoading}
            className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            <Wrench size={16} weight="bold" />
            {actionLoading ? "처리 중..." : "수리 시작 계속하기"}
          </button>
        )}
        {status === "PRODUCT_SENT" && isRequester && (
          <p className="text-xs text-slate-500">수리자가 수리를 시작하기를 기다리는 중이에요.</p>
        )}

        {status === "REPAIRING" && isRepairer && (
          <button
            type="button"
            onClick={() => runAction(`/api/fix-deals/${fixDealId}/repair-done`, "PATCH")}
            disabled={actionLoading}
            className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            <CheckCircle size={16} weight="bold" />
            {actionLoading ? "처리 중..." : "수리완료 신청"}
          </button>
        )}
        {status === "REPAIRING" && isRequester && (
          <p className="text-xs text-slate-500">수리가 진행되고 있어요. 완료 신청이 오면 결제를 진행할 수 있어요.</p>
        )}

        {status === "REPAIR_DONE" && isRequester && !paid && (
          <button
            type="button"
            onClick={() =>
              runAction("/api/payments", "POST", {
                postId,
                payeeEmail: repairerEmail,
                amount: estimatedPrice ?? 0,
              })
            }
            disabled={actionLoading}
            className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
          >
            <CreditCard size={16} weight="bold" />
            {actionLoading ? "결제 중..." : `결제하기 (${(estimatedPrice ?? 0).toLocaleString()}원)`}
          </button>
        )}
        {status === "REPAIR_DONE" && isRequester && paid && (
          <button
            type="button"
            onClick={() => runAction(`/api/fix-deals/${fixDealId}/complete`, "PATCH")}
            disabled={actionLoading}
            className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
          >
            <CheckCircle size={16} weight="bold" />
            {actionLoading ? "처리 중..." : "수리완료 수락"}
          </button>
        )}
        {status === "REPAIR_DONE" && isRepairer && (
          <p className="text-xs text-slate-500">의뢰자의 결제와 완료 수락을 기다리는 중이에요.</p>
        )}

        {status === "COMPLETED" && (
          <p className="text-xs font-semibold text-emerald-700">거래가 완료됐어요. 수고하셨습니다!</p>
        )}
      </div>
    </div>
  );
}