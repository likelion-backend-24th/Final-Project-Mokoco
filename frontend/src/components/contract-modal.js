"use client";

import { X } from "@phosphor-icons/react";
import RepairContract from "@/components/repair-contract";

export default function ContractModal({ roomId, onClose }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="sticky top-0 z-10 flex items-center justify-end border-b border-slate-100 bg-white p-3">
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="rounded-full p-1.5 text-slate-400 hover:bg-slate-100"
          >
            <X size={20} />
          </button>
        </div>
        <div className="p-2">
          <RepairContract roomId={roomId} embedded />
        </div>
      </div>
    </div>
  );
}
