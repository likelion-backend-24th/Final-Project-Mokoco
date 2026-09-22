import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function GET() {
  const accessToken = (await cookies()).get("access_token")?.value;
  if (!accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl("/notifications/settings"), {
      headers: { Authorization: `Bearer ${accessToken}` },
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) return Response.json({ error: "알림 설정을 불러오지 못했습니다." }, { status: response.status });
    return Response.json(await response.json(), { headers: { "Cache-Control": "no-store" } });
  } catch {
    return Response.json({ error: "서버에 연결하지 못했습니다." }, { status: 502 });
  }
}

export async function PUT(request) {
  const accessToken = (await cookies()).get("access_token")?.value;
  if (!accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "잘못된 요청입니다." }, { status: 400 });
  }
  try {
    const response = await fetch(backendUrl("/notifications/settings"), {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken}` },
      body: JSON.stringify({
        proposalReceived: Boolean(body.proposalReceived),
        proposalAdopted: Boolean(body.proposalAdopted),
        chatMessage: Boolean(body.chatMessage),
      }),
      signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) return Response.json({ error: "알림 설정 저장에 실패했습니다." }, { status: response.status });
    return Response.json(await response.json());
  } catch {
    return Response.json({ error: "서버에 연결하지 못했습니다." }, { status: 502 });
  }
}
