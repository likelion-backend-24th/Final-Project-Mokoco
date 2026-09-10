// 브라우저용 base. 보통 빈 값 -> 같은 오리진(상대경로)으로 호출한다. Caddy가 라우팅.
const publicBase = (process.env.NEXT_PUBLIC_BACKEND_API_URL || "").trim();
// 서버(SSR)에서만 쓰는 내부 주소. 브라우저 번들에는 포함되지 않는다.
// 공개 도메인으로 나가면 Caddy가 다시 프론트로 돌려보내 프론트가 자기 자신을 재귀 호출하게 되어 폭주한다.
const internalBase = (process.env.BACKEND_API_URL || "").trim();

function resolveBase() {
  if (typeof window === "undefined" && internalBase) return internalBase;
  return publicBase;
}

export function backendUrl(path) {
  // 이미 절대 URL이면 그대로 사용 (예: 백엔드가 내려준 이미지 전체 경로)
  if (/^https?:\/\//i.test(path)) return path;
  const p = path.startsWith("/") ? path : `/${path}`;
  const base = resolveBase();
  // base가 없으면 같은 오리진 상대경로를 그대로 반환
  return base ? new URL(p, `${base.replace(/\/$/, "")}/`).toString() : p;
}

export async function readBackendPayload(response) {
  const contentType = response.headers.get("content-type") ?? "";
  return contentType.includes("application/json") ? response.json() : response.text();
}

export function errorMessage(payload, fallback) {
  if (typeof payload === "string" && payload.trim()) return payload;
  if (payload && typeof payload.message === "string") return payload.message;
  if (payload && typeof payload.detail === "string") return payload.detail;
  return fallback;
}