"use client";

import ReportButton from "@/components/report-button";

// 수리자 이메일 팝오버의 "신고하기"에서 뜨는 모달 — 기존 ReportButton을 처음부터 펼친 채로 감싼다.
export default function RepairerReportModal({ email, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose} role="presentation">
      <div onClick={(event) => event.stopPropagation()}>
        <ReportButton authorEmail={email} defaultOpen onClose={onClose} />
      </div>
    </div>
  );
}
