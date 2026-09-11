import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function POST(request) {
  const cookieStore = await cookies();
  const email = cookieStore.get("user_email")?.value;
  const accessToken = cookieStore.get("access_token")?.value;
  if (!email || !accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  try {
    const response = await fetch(backendUrl("/payments"), {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "X-User-Email": email,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        postId: body.postId,
        payeeEmail: body.payeeEmail,
        amount: body.amount,
        baseAmount: body.baseAmount,
        paymentId: body.paymentId,
      }),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "결제를 진행하지 못했습니다."), code: payload?.code }, { status: response.status });
    }
    return Response.json({ success: true });
  } catch {
    return Response.json({ error: "결제 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." }, { status: 502 });
  }
}
