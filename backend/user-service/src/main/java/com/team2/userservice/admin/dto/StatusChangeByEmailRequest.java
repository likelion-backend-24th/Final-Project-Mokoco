package com.team2.userservice.admin.dto;

import com.team2.userservice.user.entity.AccountStatus;

public record StatusChangeByEmailRequest(String email, AccountStatus status) {
}
