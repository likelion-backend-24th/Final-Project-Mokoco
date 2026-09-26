"use client";

import { useEffect } from "react";
import { X } from "@phosphor-icons/react";
import RepairContract from "./repair-contract";

export default function ContractModal({ roomId, onClose }) {
  useEffect(() => {
    const onKey = event => { if (event.key === "Escape") onClose(); };
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKey);
    return () => { document.body.style.overflow = previous; window.removeEventListener("keydown", onKey); };
  }, [onClose]);

  return <div className="contract-modal-backdrop" role="presentation" onClick={onClose}>
    <div className="contract-modal" role="dialog" aria-modal="true" aria-label="수리 계약서" onClick={event => event.stopPropagation()}>
      <div className="contract-modal-head contract-controls">
        <h2>수리 계약서</h2>
        <button type="button" className="contract-modal-close" onClick={onClose} aria-label="닫기"><X size={20} /></button>
      </div>
      <RepairContract roomId={roomId} embedded />
    </div>
  </div>;
}
