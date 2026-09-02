package com.team2.userservice.user.dto;

import com.team2.userservice.user.entity.Role;
import com.team2.userservice.user.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String nickname;
    private Role role;
    private String region;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.nickname = user.getNickname();
        this.role = user.getRole();
        this.region = user.getRegionCode();
    }
}