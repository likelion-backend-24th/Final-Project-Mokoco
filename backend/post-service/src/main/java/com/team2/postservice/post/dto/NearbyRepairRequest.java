package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.PostStatus;
import com.team2.postservice.post.entity.RegionScope;
import com.team2.postservice.client.dto.RegionResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public record NearbyRepairRequest(Long id, String title, String content, String authorEmail,
        PostCategory category, PostStatus status, String regionCode, String regionName, LocalDateTime createdAt,
        String thumbnailUrl) {
    public record Result(List<NearbyRepairRequest> content, int number, int size,
                         long totalElements, int totalPages, boolean first, boolean last, RegionFilter regionFilter) {
        public static Result from(Page<NearbyRepairRequest> page, RegionScope scope, RegionResponse region) {
            return new Result(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                    page.getTotalPages(), page.isFirst(), page.isLast(),
                    region == null ? null : new RegionFilter(scope, region.sido(), region.sigungu(), region.dong()));
        }
    }

    public record RegionFilter(RegionScope scope, String sido, String sigungu, String dong) {}
}
