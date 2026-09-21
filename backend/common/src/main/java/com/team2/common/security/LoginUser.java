package com.team2.common.security;

public record LoginUser(Long userId, Role role) implements java.security.Principal {
    @Override public String getName() { return userId.toString(); }
    public LoginUser {
        if (userId == null || userId <= 0 || role == null) {
            throw new IllegalArgumentException("Verified user ID and role are required");
        }
    }
}
