"use client";

import { useState } from "react";
import { ClockCounterClockwise, Flag } from "@phosphor-icons/react";
import RepairerTransactionsModal from "@/components/repairer-transactions-modal";

export default function RepairerMenu({ email, nickname }) {
  const [open, setOpen] = useState(false);
  const [showTransactions, setShowTransactions] = useState(false);

  return (
    <span className="relative inline-block">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="text-sm font-bold text-slate-800 underline decoration-slate-300 underline-offset-2 hover:decoration-slate-500"
      >
        {nickname || email || "수리공 이웃"}
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} role="presentation" />
          <div className="absolute left-0 top-[26px] z-20 w-44 overflow-hidden rounded-lg border border-slate-200 bg-white shadow-lg">
            <button
              type="button"
              onClick={() => {
                setShowTransactions(true);
                setOpen(false);
              }}
              className="flex w-full items-center gap-2 px-3 py-2.5 text-left text-xs text-slate-700 hover:bg-slate-50"
            >
              <ClockCounterClockwise size={16} className="text-slate-400" />
              거래 내역 보기
            </button>
            <div className="border-t border-slate-100" />
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="flex w-full items-center gap-2 px-3 py-2.5 text-left text-xs text-red-500 hover:bg-red-50"
            >
              <Flag size={16} />
              신고하기
            </button>
          </div>
        </>
      )}

      {showTransactions && (
        <RepairerTransactionsModal email={email} onClose={() => setShowTransactions(false)} />
      )}
    </span>
  );
}