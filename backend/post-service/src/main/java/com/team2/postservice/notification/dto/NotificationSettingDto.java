package com.team2.postservice.notification.dto;

import com.team2.postservice.notification.entity.NotificationSetting;

public record NotificationSettingDto(
        boolean proposalReceived,
        boolean proposalAdopted,
        boolean chatMessage
) {
    public static NotificationSettingDto from(NotificationSetting s) {
        return new NotificationSettingDto(s.isProposalReceived(), s.isProposalAdopted(), s.isChatMessage());
    }

    public static NotificationSettingDto defaults() {
        return new NotificationSettingDto(true, true, true);
    }
}
