import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function POST(request, { params }) {
  const { roomId } = await params;
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  if (!/^\d+$/.test(roomId)) return Response.json({ error: "잘못된 채팅방입니다." }, { status: 400 });
  if (Number(request.headers.get("content-length")) > 51 * 1024 * 1024)
    return Response.json({ error: "파일은 50MB 이하여야 합니다." }, { status: 413 });
  try {
    const form = await request.formData();
    const file = form.get("file");
    if (!file || typeof file === "string" || file.size > 50 * 1024 * 1024)
      return Response.json({ error: "50MB 이하 파일을 선택해주세요." }, { status: 400 });
    const body = new FormData(); body.set("file", file);
    const response = await fetch(backendUrl(`/api/chat-rooms/${roomId}/attachments`), {
      method: "POST", headers: { Authorization: `Bearer ${token}` }, body, signal: AbortSignal.timeout(120000),
    });
    if (!response.ok) return Response.json({ error: response.status === 413 ? "이미지는 10MB, 동영상은 50MB까지 가능합니다." : response.status === 415 ? "JPG, PNG, GIF, WebP, MP4, WebM만 지원합니다." : "파일을 전송하지 못했습니다. 로그인 및 채팅방 권한을 확인해주세요." }, { status: response.status });
    return Response.json(await response.json());
  } catch { return Response.json({ error: "전송 결과를 확인하지 못했습니다. 대화 내역 확인 후 다시 시도해주세요." }, { status: 502 }); }
}
