import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function contractProxy(request, context, method) {
  const { roomId, action } = await context.params;
  if (!/^\d+$/.test(roomId) || (action && !["request", "sign", "start", "finish", "accept"].includes(action)))
    return Response.json({ error: "잘못된 요청입니다." }, { status: 400 });
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl(`/api/chat-rooms/${roomId}/contract${action ? `/${action}` : ""}`), {
      method, headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: method === "POST" ? JSON.stringify(await request.json()) : undefined,
      cache: "no-store", signal: AbortSignal.timeout(15000),
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) return Response.json({ error: payload?.error || payload?.message || "계약 요청에 실패했습니다." }, { status: response.status });
    return Response.json(payload, { headers: { "Cache-Control": "no-store" } });
  } catch { return Response.json({ error: "서버 연결에 실패했습니다. 계약 상태 확인 후 다시 시도해주세요." }, { status: 502 }); }
}
