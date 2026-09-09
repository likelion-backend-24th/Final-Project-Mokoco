import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function PATCH(request, { params }) {
  const { fixDealId } = await params;
  const email = (await cookies()).get("user_email")?.value;
  if (!email) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  try {
    const response = await fetch(backendUrl(`/fix-deals/${fixDealId}/complete`), {
      method: "PATCH",
      headers: { "X-User-Email": email },
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "수리완료 수락에 실패했습니다."), code: payload?.code }, { status: response.status });
    }
    return Response.json({ success: true });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." }, { status: 502 });
  }
}
