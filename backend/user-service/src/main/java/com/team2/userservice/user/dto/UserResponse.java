package com.team2.userservice.user.dto;

import com.team2.userservice.user.entity.Role;
import com.team2.userservice.user.entity.User;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String nickname;
    private Role role;
    private String regionCode;
    private String regionName; // 화면에 보여줄 읽기 쉬운 동네 이름

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.nickname = user.getNickname();
        this.role = user.getRole();

        if (user.getRegion() != null) {
            this.regionCode = user.getRegion().getRegionCode();
            // 시도 + 시군구 + 동을 조합해서 하나의 문자열로 제공
            this.regionName = String.format("%s %s %s",
                    user.getRegion().getSido(),
                    user.getRegion().getSigungu(),
                    user.getRegion().getDong()
            ).trim();
        }
    }
}
