import { aiProxy } from "@/lib/ai-proxy";
export async function POST(request, { params }) {
  const { roomId } = await params;
  if (!/^\d+$/.test(roomId)) return Response.json({ message: "잘못된 채팅방입니다." }, { status: 400 });
  return aiProxy(request, `/api/chat-rooms/${roomId}/contract/ai-draft`);
}
