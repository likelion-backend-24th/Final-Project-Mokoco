import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function GET() {
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl("/api/chat-rooms/session"), {
      headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) {
      const error = response.status === 404
        ? "채팅 인증 API를 찾을 수 없습니다. post-service가 최신 코드로 실행 중인지 확인해주세요."
        : response.status === 401 || response.status === 403
          ? "인증에 실패했습니다. 로그인 상태와 서비스 인증 설정을 확인해주세요."
          : "채팅 인증 서버에서 오류가 발생했습니다. 서버 로그를 확인해주세요.";
      return Response.json({ error }, { status: response.status });
    }
    const user = await response.json();
    return Response.json({ token, userId: user.id }, { headers: { "Cache-Control": "no-store" } });
  } catch { return Response.json({ error: "인증 서버에 연결하지 못했습니다." }, { status: 502 }); }
}
