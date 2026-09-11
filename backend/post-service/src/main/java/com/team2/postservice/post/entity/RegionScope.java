package com.team2.postservice.post.entity;

import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;

/** Administrative region codes: 2-digit province, 5-digit district, full dong code. */
public enum RegionScope {
    /** 지역 필터를 아예 적용하지 않음 (기본값) — 활동 지역 설정 여부와 무관하게 전체 글을 보여준다. */
    ALL, SIDO, SIGUNGU, DONG;

    public String queryPattern(String regionCode) {
        if (this == ALL) return null;
        if (regionCode == null || !regionCode.matches("[0-9]{8}([0-9]{2})?"))
            throw new CustomException(ErrorCode.ACTIVITY_REGION_REQUIRED);
        return switch (this) {
            case SIDO -> regionCode.substring(0, 2) + "%";
            case SIGUNGU -> regionCode.substring(0, 5) + "%";
            case DONG -> regionCode;
            case ALL -> null;
        };
    }
}
