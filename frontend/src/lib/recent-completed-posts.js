export async function getRecentCompletedPosts() {
  try {
    const response = await fetch(
      `${process.env.BACKEND_URL}/api/posts/completed/recent?size=3`,
      {
        cache: "no-store",
      }
    );

    if (!response.ok) {
      return [];
    }

    return await response.json();
  } catch (error) {
    console.error("최근 완료 게시글 조회 실패:", error);
    return [];
  }
}
