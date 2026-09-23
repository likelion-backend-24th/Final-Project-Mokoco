import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

// 신고 접수함(USER 신고)에서 대상 유저 id 없이 이메일만으로 바로 정지시키기 위한 경로.
export async function PATCH(request) {
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "잘못된 요청입니다." }, { status: 400 });
  }
  if (!body?.email || (body.status !== "ACTIVE" && body.status !== "SUSPENDED")) {
    return Response.json({ error: "email과 status(ACTIVE 또는 SUSPENDED)가 필요합니다." }, { status: 400 });
  }
  try {
    const response = await fetch(backendUrl("/api/admin/users/by-email/status"), {
      method: "PATCH",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ email: body.email, status: body.status }),
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "상태 변경에 실패했습니다.") }, { status: response.status });
    }
    return Response.json(payload);
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
