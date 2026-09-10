const rawBase = process.env.NEXT_PUBLIC_BACKEND_API_URL;
const baseUrl = (rawBase && rawBase.trim()) ? rawBase : "http://32.199.114.190";

export function backendUrl(path) { 
  return new URL(path, `${baseUrl.replace(/\/$/, "")}/`).toString(); 
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