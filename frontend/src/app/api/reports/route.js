import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function POST(request) {
  const email = (await cookies()).get("user_email")?.value;
  if (!email) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "잘못된 요청입니다." }, { status: 400 });
  }
  try {
    const response = await fetch(backendUrl("/api/reports"), {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-User-Email": email },
      body: JSON.stringify(body),
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "신고 접수에 실패했습니다.") }, { status: response.status });
    }
    return Response.json({ success: true });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
