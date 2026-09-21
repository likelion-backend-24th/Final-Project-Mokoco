package com.team2.common.security;

/** Identity returned by user-service after verifying the access token. Never populate from client identity headers. */
public record LoginUser(Long id, String email) {
    public LoginUser(Long id) { this(id, null); }
}
