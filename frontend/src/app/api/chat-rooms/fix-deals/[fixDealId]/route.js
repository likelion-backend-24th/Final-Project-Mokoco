import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

async function forward(method, { params }) {
  const { fixDealId } = await params;
  if (!/^\d+$/.test(fixDealId)) {
    return Response.json({ error: "잘못된 거래 번호입니다." }, { status: 400 });
  }
  const email = (await cookies()).get("user_email")?.value;
  if (!email) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl(`/api/chat-rooms/fix-deals/${fixDealId}`), {
      method,
      headers: { "X-User-Email": email },
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "채팅방 요청에 실패했습니다."), code: payload?.code }, { status: response.status });
    }
    return Response.json(payload);
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." }, { status: 502 });
  }
}

export function GET(request, context) { return forward("GET", context); }
export function POST(request, context) { return forward("POST", context); }
