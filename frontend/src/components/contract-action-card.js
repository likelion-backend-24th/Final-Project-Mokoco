"use client";
import { useCallback, useEffect, useState } from "react";
import * as PortOne from "@portone/browser-sdk/v2";
import ReviewForm from "./review-form";

const FEE_RATE = 0.1; // 백엔드 Payment.calculateFee와 동일 (견적 금액의 10%)

function calculateSettlement(baseAmount) {
  const base = baseAmount ?? 0;
  const fee = Math.round(base * FEE_RATE);
  return { base, fee, net: base - fee };
}

const buttonClass = "rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-semibold text-white disabled:opacity-50";

// 프로필의 거래 카드 안에 계약서 페이지의 "다음 할 일"(결제/작업 진행/후기)만 축약해서 보여준다.
// 계약서 전문·서명 절차는 여전히 "계약서 보기"에서 하고, 여기서는 서명이 끝난 뒤의 액션만 다룬다.
export default function ContractActionCard({ roomId }) {
  const [overview, setOverview] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [userId, setUserId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  const [paymentBusy, setPaymentBusy] = useState(false);
  const [paymentError, setPaymentError] = useState("");
  const [reviewSubmitted, setReviewSubmitted] = useState(false);
  const endpoint = `/api/chat-rooms/${roomId}/contract`;

  const load = useCallback(async () => {
    const response = await fetch(endpoint, { cache: "no-store" });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error);
    setOverview(data);
    return data;
  }, [endpoint]);

  useEffect(() => {
    let active = true;
    async function refresh() { try { if (active) await load(); } catch { if (active) setLoadFailed(true); } }
    refresh();
    fetch("/api/chat/session", { cache: "no-store" })
      .then(async (response) => {
        const data = await response.json();
        if (!response.ok) throw new Error(data.error);
        if (active) setUserId(data.userId);
      })
      .catch(() => {});
    return () => { active = false; };
  }, [load]);

  useEffect(() => {
    if (overview?.dealStatus !== "COMPLETED" || !overview?.postId) return;
    let active = true;
    fetch(`/api/reviews/exists?postId=${overview.postId}`, { cache: "no-store" })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => { if (active) setReviewSubmitted(Boolean(data?.exists)); })
      .catch(() => {});
    return () => { active = false; };
  }, [overview?.dealStatus, overview?.postId]);

  async function startPayment() {
    setPaymentBusy(true); setPaymentError("");
    try {
      const paymentId = `payment-${crypto.randomUUID()}`;
      const base = overview.estimatedPrice ?? 0;

      const paymentResult = await PortOne.requestPayment({
        storeId: process.env.NEXT_PUBLIC_PORTONE_STORE_ID,
        channelKey: process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY,
        paymentId,
        orderName: "동네수리 - 수리 대금 안전결제",
        totalAmount: base,
        currency: "CURRENCY_KRW",
        payMethod: "CARD",
        isEscrow: true,
        customer: overview.requesterEmail ? { email: overview.requesterEmail } : undefined,
        customData: JSON.stringify({ postId: overview.postId, payerEmail: overview.requesterEmail, payeeEmail: overview.repairerEmail, baseAmount: base }),
        noticeUrls: [`${window.location.origin}/api/payments/webhook`],
      });

      if (paymentResult?.code != null) {
        setPaymentError(paymentResult.message ?? "결제가 취소되었거나 실패했습니다.");
        return;
      }

      const confirmRes = await fetch("/api/payments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          postId: overview.postId,
          payeeEmail: overview.repairerEmail,
          amount: base,
          baseAmount: base,
          paymentId,
        }),
      });
      const confirmData = await confirmRes.json().catch(() => ({}));
      if (!confirmRes.ok && confirmData.code !== "DUPLICATE_PAYMENT") {
        setPaymentError(confirmData.error ?? "결제 확인에 실패했습니다. 잠시 후 다시 확인해주세요.");
        return;
      }

      await load();
    } catch {
      setPaymentError("결제 진행 중 문제가 발생했습니다.");
    } finally {
      setPaymentBusy(false);
    }
  }

  async function advance(action, prompt) {
    if (!window.confirm(prompt)) return;
    setBusy(true); setActionError("");
    try {
      const response = await fetch(`${endpoint}/${action}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ versionId: overview.versions[0]?.id }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error);
      await load();
    } catch (failure) {
      setActionError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  if (loadFailed || !overview) return null;

  const latest = overview.versions[0];
  // 서명 전(초안/서명 대기) 단계는 문서·서명 절차가 필요해 "계약서 보기"에서 진행 — 여기선 그 이후 액션만.
  if (!latest || latest.status !== "SIGNED" || overview.dealStatus === "CANCELED") return null;

  const paid = overview.payment?.status === "COMPLETED";
  const settlement = calculateSettlement(overview.estimatedPrice);
  const isRequester = userId === overview.requesterId;
  const isRepairer = userId === overview.repairerId;

  return (
    <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-3">
      {overview.dealStatus === "MATCHED" && isRequester && !paid && (
        <div>
          <p className="text-sm text-slate-600">결제하면 수리자가 작업을 시작할 수 있어요.</p>
          <button type="button" disabled={paymentBusy} onClick={startPayment} className={`mt-2 ${buttonClass}`}>
            {paymentBusy ? "결제 확인 중..." : `안전결제 하기 (${settlement.base.toLocaleString("ko-KR")}원)`}
          </button>
          {paymentError && <p role="alert" className="mt-1.5 text-xs text-red-600">{paymentError}</p>}
        </div>
      )}
      {overview.dealStatus === "MATCHED" && isRepairer && !paid && (
        <p className="text-sm text-slate-500">의뢰인의 결제를 기다리고 있어요.</p>
      )}
      {overview.dealStatus === "MATCHED" && paid && !isRepairer && (
        <p className="text-sm text-slate-500">결제 완료 — 수리자의 작업 시작을 기다리고 있어요.</p>
      )}
      {overview.dealStatus === "MATCHED" && isRepairer && paid && (
        <button type="button" disabled={busy} className={buttonClass}
          onClick={() => advance("start", "체결된 계약에 따라 수리 작업을 시작하시겠습니까?")}>
          수리 작업 시작
        </button>
      )}
      {overview.dealStatus === "REPAIRING" && isRepairer && (
        <button type="button" disabled={busy} className={buttonClass}
          onClick={() => advance("finish", "작업을 마치고 의뢰인에게 완료 확인을 요청하시겠습니까?")}>
          작업 완료 확인 요청
        </button>
      )}
      {overview.dealStatus === "REPAIRING" && !isRepairer && (
        <p className="text-sm text-slate-500">수리 작업이 진행 중이에요.</p>
      )}
      {overview.dealStatus === "REPAIR_DONE" && isRequester && (
        <button type="button" disabled={busy} className={buttonClass}
          onClick={() => advance("accept", "계약의 검수 기준을 확인하고 수리 완료를 수락하시겠습니까?")}>
          검수 및 수리 완료 확인
        </button>
      )}
      {overview.dealStatus === "REPAIR_DONE" && !isRequester && (
        <p className="text-sm text-slate-500">의뢰인의 완료 확인을 기다리고 있어요.</p>
      )}
      {overview.dealStatus === "COMPLETED" && isRequester && !reviewSubmitted && (
        <ReviewForm postId={overview.postId} onSubmitted={() => setReviewSubmitted(true)} />
      )}
      {overview.dealStatus === "COMPLETED" && isRequester && reviewSubmitted && (
        <p className="text-sm text-slate-500">후기 작성 완료 — 남겨주셔서 감사해요.</p>
      )}
      {overview.dealStatus === "COMPLETED" && isRepairer && (
        <p className="text-sm text-slate-500">거래 완료 — 정산 내역에서 확인할 수 있어요.</p>
      )}
      {actionError && <p role="alert" className="mt-1.5 text-xs text-red-600">{actionError}</p>}
    </div>
  );
}
