import { backendUrl, readBackendPayload } from "./backend.js";

const ACTIVE_STATUSES = new Set(["MATCHED", "REPAIRING", "REPAIR_DONE"]);

async function fetchRole(accessToken, role) {
  const response = await fetch(backendUrl(`/profile/transactions?role=${role}&page=0&size=10`), {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store", signal: AbortSignal.timeout(8000),
  });
  const payload = await readBackendPayload(response);
  if (!response.ok || !Array.isArray(payload?.items)) return [];
  return payload.items.filter((item) => ACTIVE_STATUSES.has(item.status));
}

function sortRecent(items) {
  return items.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 5);
}

// 홈 화면에 보여줄, 로그인한 사용자가 지금 진행 중인 거래를 의뢰자/수리자 역할별로 나눠서 돌려준다.
// 실패하거나 로그인 전이면 빈 목록만 돌려주고 홈 화면 자체는 그대로 뜨게 한다.
export async function getMyActiveDeals(accessToken) {
  if (!accessToken) return { requester: [], repairer: [] };
  try {
    const [requester, repairer] = await Promise.all([
      fetchRole(accessToken, "requester"),
      fetchRole(accessToken, "repairer"),
    ]);
    return { requester: sortRecent(requester), repairer: sortRecent(repairer) };
  } catch {
    return { requester: [], repairer: [] };
  }
}
