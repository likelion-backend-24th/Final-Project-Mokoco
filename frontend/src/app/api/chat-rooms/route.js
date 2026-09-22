import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function GET(request) {
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  const search = new URL(request.url).searchParams;
  const page = search.get("page") ?? "0";
  const size = search.get("size") ?? "5";
  if (!/^\d+$/.test(page) || !/^\d+$/.test(size) || Number(page) > 2147483647 || Number(size) < 1 || Number(size) > 50) {
    return Response.json({ error: "페이지는 0 이상, 조회 개수는 1~50이어야 합니다." }, { status: 400 });
  }
  try {
    const response = await fetch(backendUrl(`/api/chat-rooms?page=${page}&size=${size}`), {
      headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) return Response.json({ error: response.status === 401 ? "다시 로그인해주세요." : "채팅방 목록을 불러오지 못했습니다." }, { status: response.status });
    return Response.json(await response.json(), { headers: { "Cache-Control": "no-store" } });
  } catch {
    return Response.json({ error: "채팅 서버에 연결하지 못했습니다." }, { status: 502 });
  }
}
