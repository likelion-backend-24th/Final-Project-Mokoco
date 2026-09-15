import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function PATCH(request, { params }) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) return Response.json({ error: "잘못된 요청입니다." }, { status: 400 });
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl(`/api/admin/reports/${id}/resolve`), {
      method: "PATCH",
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "처리에 실패했습니다.") }, { status: response.status });
    }
    return Response.json(payload);
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
