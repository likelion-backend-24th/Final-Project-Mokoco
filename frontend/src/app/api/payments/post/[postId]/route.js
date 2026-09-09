import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function GET(request, { params }) {
  const { postId } = await params;
  const email = (await cookies()).get("user_email")?.value;
  if (!email) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  try {
    const response = await fetch(backendUrl(`/payments/post/${postId}`), {
      headers: { "X-User-Email": email },
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "결제 정보를 불러오지 못했습니다."), code: payload?.code }, { status: response.status });
    }
    return Response.json(payload);
  } catch {
    return Response.json({ error: "결제 서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
