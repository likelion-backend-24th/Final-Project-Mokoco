import { backendUrl, errorMessage, readBackendPayload } from "./backend.js";

export async function getNearbyPosts(accessToken, category = "ALL", page = 0, size = 20, regionScope = "ALL") {
  try {
    const query = new URLSearchParams({ category, page: String(page), size: String(size) });
    if (accessToken) query.set("regionScope", regionScope);
    const response = await fetch(backendUrl(`/posts?${query}`), {
      headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) return { posts: [], error: errorMessage(payload, "수리 요청을 불러오지 못했습니다.") };
    if (!Array.isArray(payload?.content)) return { posts: [], error: "수리 요청 응답 형식이 올바르지 않습니다." };
    return { posts: payload.content, pagination: payload, error: null };
  } catch {
    return { posts: [], error: "수리 요청 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." };
  }
}
