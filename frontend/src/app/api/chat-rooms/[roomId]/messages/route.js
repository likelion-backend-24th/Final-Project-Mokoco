import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function GET(request, { params }) {
  const { roomId } = await params;
  const before = new URL(request.url).searchParams.get("before");
  if (!/^\d+$/.test(roomId) || (before && !/^\d+$/.test(before))) return Response.json({ error: "잘못된 요청입니다." }, { status: 400 });
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl(`/api/chat-rooms/${roomId}/messages${before ? `?before=${before}` : ""}`), {
      headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) return Response.json({ error: response.status === 403 ? "이 채팅방에 참여할 수 없습니다." : "메시지를 불러오지 못했습니다." }, { status: response.status });
    return Response.json(await response.json(), { headers: { "Cache-Control": "no-store" } });
  } catch { return Response.json({ error: "서버에 연결하지 못했습니다." }, { status: 502 }); }
}
