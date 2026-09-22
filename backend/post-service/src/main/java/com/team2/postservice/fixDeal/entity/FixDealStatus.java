package com.team2.postservice.fixDeal.entity;

public enum FixDealStatus {

    MATCHED,        // 제안 채택, 거래 성립 (계약 서명 + 결제 후 REPAIRING으로 전이)

    /** @deprecated 레거시 상태 — 신규 거래에서는 사용하지 않음. 기존 행 조회 호환용으로만 유지한다. */
    @Deprecated
    PRODUCT_SENT,   // (구) 제품 전달 완료

    REPAIRING,      // 수리 진행 중
    REPAIR_DONE,    // 수리자가 수리 완료 신청
    COMPLETED,      // 의뢰자가 수리 완료 수락 (정산 확정)
    CANCELED        // 거래 취소
}
