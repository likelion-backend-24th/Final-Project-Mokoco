package com.team2.postservice.post.entity;

import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;

/** Administrative region codes: 2-digit province, 5-digit district, full dong code. */
public enum RegionScope {
    SIDO, SIGUNGU, DONG;

    public String queryPattern(String regionCode) {
        if (regionCode == null || !regionCode.matches("[0-9]{8}([0-9]{2})?"))
            throw new CustomException(ErrorCode.ACTIVITY_REGION_REQUIRED);
        return switch (this) {
            case SIDO -> regionCode.substring(0, 2) + "%";
            case SIGUNGU -> regionCode.substring(0, 5) + "%";
            case DONG -> regionCode;
        };
    }
}
