package com.team2.userservice.region.controller;

import com.team2.userservice.region.dto.RegionRequest;
import com.team2.userservice.region.dto.RegionResponse;
import com.team2.userservice.region.service.RegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/region")
public class RegionController {

    private final RegionService regionService;

    @PatchMapping
    public ResponseEntity<RegionResponse> updateRegion(
            @AuthenticationPrincipal String email,
            @RequestBody @Valid RegionRequest request
    ) {
        RegionResponse response =
                regionService.setMyRegion(email, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<RegionResponse> getRegion(
            @AuthenticationPrincipal String email
    ) {
        RegionResponse response = regionService.getRegionInfo(email);
        if (response == null) {
            return ResponseEntity.notFound().build(); // 👈 지역이 없으면 404를 반환해야 프론트가 동네 설정 모달을 띄웁니다!
        }
        return ResponseEntity.ok(response);
    }
}
