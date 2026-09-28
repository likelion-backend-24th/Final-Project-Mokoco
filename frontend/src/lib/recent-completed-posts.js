import { backendUrl } from "./backend.js";

export async function getRecentCompletedPosts() {
  try {
    const response = await fetch(
      backendUrl("/posts/completed/recent?size=3"),
      {
        cache: "no-store",
        signal: AbortSignal.timeout(10000),
      }
    );

    if (!response.ok) {
      console.error(
        "최근 완료 게시글 조회 실패:",
        response.status
      );
      return [];
    }

    return await response.json();
  } catch (error) {
    console.error(
      "최근 완료 게시글 조회 실패:",
      error
    );
    return [];
  }
}
