package com.team2.userservice.region.controller;

import com.team2.userservice.region.dto.RegionResponse;
import com.team2.userservice.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
public class InternalRegionController {

    private final RegionService regionService;

    // Post 서비스 등 다른 MSA 컴포넌트가 이메일로 지역 정보를 가져갈 수 있는 내부 API
    @GetMapping("/{email}/region")
    public RegionResponse getRegionByEmail(@PathVariable("email") String email) {
        return regionService.getRegionInfo(email);
    }
}