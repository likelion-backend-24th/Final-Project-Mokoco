public record RecentCompletedDealResponse(
        Long dealId,
        Long postId,
        String title,
        String category,
        String regionName,
        String thumbnailUrl,
        Long finalPrice,
        LocalDateTime completedAt
) {
}
