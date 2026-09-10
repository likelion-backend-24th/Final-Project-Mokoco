const publicBase = (process.env.NEXT_PUBLIC_BACKEND_API_URL || "").trim() || "http://32.199.114.190";
// 서버(SSR)에서 백엔드를 부를 때 쓰는 내부 주소. 브라우저 번들에는 포함되지 않는다.
// 공개 IP로 나가면 nginx의 `location /` 로 되돌아와 프론트가 자기 자신을 재귀 호출하게 되어 폭주한다.
const internalBase = (process.env.BACKEND_API_URL || "").trim();

function resolveBase() {
  if (typeof window === "undefined" && internalBase) return internalBase;
  return publicBase;
}

export function backendUrl(path) {
  return new URL(path, `${resolveBase().replace(/\/$/, "")}/`).toString();
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