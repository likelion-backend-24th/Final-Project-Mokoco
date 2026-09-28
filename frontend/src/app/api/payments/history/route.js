import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

// 마이페이지용 내 결제 이력 전체 (의뢰자로 결제한 건 + 수리자로 정산받은 건, 역할 구분 없이).
// /api/payments/mine(수리자 정산 요약)과는 다른 화면이라 경로를 분리했다.
export async function GET() {
  const email = (await cookies()).get("user_email")?.value;
  if (!email) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  try {
    const response = await fetch(backendUrl("/payments/history"), {
      headers: { "X-User-Email": email },
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json(
        { error: errorMessage(payload, "결제 이력을 불러오지 못했습니다."), code: payload?.code },
        { status: response.status },
      );
    }
    return Response.json(payload);
  } catch {
    return Response.json({ error: "결제 서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
