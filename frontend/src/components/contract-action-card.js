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
      const prepareRes = await fetch("/api/payments/prepare", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ postId: overview.postId }),
      });
      const order = await prepareRes.json().catch(() => ({}));
      if (!prepareRes.ok) {
        setPaymentError(order.error ?? "결제를 준비하지 못했습니다.");
        return;
      }

      const paymentResult = await PortOne.requestPayment({
        storeId: process.env.NEXT_PUBLIC_PORTONE_STORE_ID,
        channelKey: process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY,
        paymentId: order.paymentId,
        orderName: "동네수리 - 수리 대금 안전결제",
        totalAmount: order.totalAmount,
        currency: "CURRENCY_KRW",
        payMethod: "CARD",
        isEscrow: true,
        customer: overview.requesterEmail ? { email: overview.requesterEmail } : undefined,
        // paymentId가 서버가 미리 만들어둔 주문(PaymentOrder)에 연결돼있어, 결제 확정/웹훅이
        // 브라우저가 실어보내는 customData가 아니라 그 주문을 유일한 진실 소스로 삼는다.
        noticeUrls: [`${window.location.origin}/api/payments/webhook`],
      });

      if (paymentResult?.code != null) {
        setPaymentError(paymentResult.message ?? "결제가 취소되었거나 실패했습니다.");
        return;
      }

      const confirmRes = await fetch("/api/payments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ postId: overview.postId, paymentId: order.paymentId }),
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
  const cancelled = overview.payment?.status === "CANCELLED";
  const settlement = calculateSettlement(overview.estimatedPrice);
  const isRequester = userId === overview.requesterId;
  const isRepairer = userId === overview.repairerId;

  const contractAmount = Number(latest.terms?.totalAmount);
  const amountMismatch = overview.estimatedPrice != null && !Number.isNaN(contractAmount) && contractAmount !== overview.estimatedPrice;

  return (
    <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-3">
      <p className="text-sm font-bold text-slate-800">계약 체결 완료</p>
      <p className="mt-1 text-sm text-slate-500">양측 서명이 완료되었습니다. 위 계약 내용을 기준으로 작업을 진행하세요.</p>
      {amountMismatch && (
        <p className="mt-1.5 text-xs text-red-600">
          계약서 금액({contractAmount.toLocaleString("ko-KR")}원)과 채택된 견적 금액({overview.estimatedPrice.toLocaleString("ko-KR")}원)이 달라요. 결제는 채택된 견적 금액 기준으로 진행됩니다.
        </p>
      )}
      {overview.dealStatus === "MATCHED" && isRequester && !paid && (
        <div className="mt-2">
          {cancelled && (
            <p className="mb-1.5 text-sm font-semibold text-amber-600">이전 결제가 취소되었습니다. 다시 결제해주세요.</p>
          )}
          <p className="text-sm text-slate-600">계약이 체결되었습니다. 결제하면 수리자가 작업을 시작할 수 있어요. 결제 금액은 완료될 때까지 안전하게 보관됩니다.</p>
          <button type="button" disabled={paymentBusy} onClick={startPayment} className={`mt-2 ${buttonClass}`}>
            {paymentBusy ? "결제 확인 중..." : `안전결제 하기 (${settlement.base.toLocaleString("ko-KR")}원)`}
          </button>
          <p className="mt-1.5 text-xs text-slate-400">
            견적 금액 그대로 결제돼요. 거래 완료 시 플랫폼 수수료(10%) {settlement.fee.toLocaleString("ko-KR")}원을 제외한 {settlement.net.toLocaleString("ko-KR")}원이 수리자에게 정산됩니다.
          </p>
          {paymentError && <p role="alert" className="mt-1.5 text-xs text-red-600">{paymentError}</p>}
        </div>
      )}
      {overview.dealStatus === "MATCHED" && isRepairer && !paid && (
        <p className="mt-2 text-sm text-slate-500">{cancelled ? "결제가 취소되어 의뢰인의 재결제를 기다리고 있어요." : "의뢰인의 결제를 기다리고 있어요."}</p>
      )}
      {overview.dealStatus === "MATCHED" && paid && !isRepairer && (
        <p className="mt-2 text-sm text-slate-500">결제 완료 — 수리자의 작업 시작을 기다리고 있어요.</p>
      )}
      {overview.dealStatus === "MATCHED" && isRepairer && paid && (
        <button type="button" disabled={busy} className={`mt-2 ${buttonClass}`}
          onClick={() => advance("start", "체결된 계약에 따라 수리 작업을 시작하시겠습니까?")}>
          수리 작업 시작
        </button>
      )}
      {overview.dealStatus === "REPAIRING" && isRepairer && (
        <button type="button" disabled={busy} className={`mt-2 ${buttonClass}`}
          onClick={() => advance("finish", "작업을 마치고 의뢰인에게 완료 확인을 요청하시겠습니까?")}>
          작업 완료 확인 요청
        </button>
      )}
      {overview.dealStatus === "REPAIRING" && !isRepairer && (
        <p className="mt-2 text-sm text-slate-500">수리 작업이 진행 중이에요.</p>
      )}
      {overview.dealStatus === "REPAIR_DONE" && isRequester && (
        <button type="button" disabled={busy} className={`mt-2 ${buttonClass}`}
          onClick={() => advance("accept", "계약의 검수 기준을 확인하고 수리 완료를 수락하시겠습니까?")}>
          검수 및 수리 완료 확인
        </button>
      )}
      {overview.dealStatus === "REPAIR_DONE" && !isRequester && (
        <p className="mt-2 text-sm text-slate-500">의뢰인의 완료 확인을 기다리고 있어요.</p>
      )}
      {overview.dealStatus === "COMPLETED" && isRequester && !reviewSubmitted && (
        <div className="mt-2"><ReviewForm postId={overview.postId} onSubmitted={() => setReviewSubmitted(true)} /></div>
      )}
      {overview.dealStatus === "COMPLETED" && isRequester && reviewSubmitted && (
        <p className="mt-2 text-sm text-slate-500">후기 작성 완료 — 남겨주셔서 감사해요.</p>
      )}
      {overview.dealStatus === "COMPLETED" && isRepairer && (
        <p className="mt-2 text-sm text-slate-500">거래 완료 — 정산 내역에서 확인할 수 있어요.</p>
      )}
      {actionError && <p role="alert" className="mt-1.5 text-xs text-red-600">{actionError}</p>}
    </div>
  );
}
