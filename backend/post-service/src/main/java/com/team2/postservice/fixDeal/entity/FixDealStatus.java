package com.team2.postservice.fixDeal.entity;

public enum FixDealStatus {

    MATCHED,        // 제안 채택, 거래 성립
    PRODUCT_SENT,   // 제품 전달 완료
    REPAIRING,      // 수리 진행 중
    REPAIR_DONE,    // 수리자가 수리 완료 신청
    COMPLETED,      // 의뢰자가 수리 완료 수락
    CANCELED        // 거래 취소
}