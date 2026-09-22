import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function DELETE(request, { params }) {
  const { roomId, messageId } = await params;
  if (!/^\d+$/.test(roomId) || !/^\d+$/.test(messageId)) return Response.json({ error: "잘못된 메시지입니다." }, { status: 400 });
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl(`/api/chat-rooms/${roomId}/messages/${messageId}`), {
      method: "DELETE", headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) return Response.json({ error: response.status === 403 ? "본인이 보낸 메시지만 삭제할 수 있습니다." : "메시지를 삭제하지 못했습니다." }, { status: response.status });
    return Response.json(await response.json());
  } catch { return Response.json({ error: "서버에 연결하지 못했습니다. 다시 시도해주세요." }, { status: 502 }); }
}
