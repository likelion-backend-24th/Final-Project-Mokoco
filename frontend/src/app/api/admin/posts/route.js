import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

// 관리자 글 관리 목록. keyword/status/page/size 쿼리를 그대로 백엔드에 전달한다.
export async function GET(request) {
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  const { searchParams } = new URL(request.url);
  const query = new URLSearchParams();
  if (searchParams.get("keyword")) query.set("keyword", searchParams.get("keyword"));
  if (searchParams.get("status")) query.set("status", searchParams.get("status"));
  query.set("page", searchParams.get("page") ?? "0");
  query.set("size", searchParams.get("size") ?? "20");

  try {
    const response = await fetch(backendUrl(`/api/admin/posts?${query.toString()}`), {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "글 목록을 불러오지 못했습니다.") }, { status: response.status });
    }
    return Response.json(payload, { headers: { "Cache-Control": "no-store" } });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
