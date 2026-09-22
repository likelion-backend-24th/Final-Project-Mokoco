import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function POST(request) {
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  let body;
  try { body = await request.json(); }
  catch { return Response.json({ error: "잘못된 요청입니다." }, { status: 400 }); }
  try {
    const response = await fetch(backendUrl("/payments/prepare"), {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ postId: body.postId }),
      cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) return Response.json({ error: errorMessage(payload, "결제를 준비하지 못했습니다.") }, { status: response.status });
    return Response.json(payload);
  } catch { return Response.json({ error: "결제 서버에 연결하지 못했습니다." }, { status: 502 }); }
}
