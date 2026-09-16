import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function PATCH(request) {
  const accessToken = (await cookies()).get("access_token")?.value;
  if (!accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  try {
    const response = await fetch(backendUrl("/api/users/me/password"), {
      method: "PATCH",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({ currentPassword: body.currentPassword, newPassword: body.newPassword }),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "비밀번호를 변경하지 못했습니다.") }, { status: response.status });
    }
    return Response.json({ success: true });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
