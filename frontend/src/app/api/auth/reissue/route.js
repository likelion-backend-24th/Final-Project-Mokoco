import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { jwtDecode } from "jwt-decode";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

// 액세스 토큰 재발급. 클라이언트(AuthInitializer)가 만료 임박 시 호출한다.
// refresh_token 은 httpOnly 쿠키라 서버에서만 읽을 수 있으므로 이 라우트가 필요.
export async function POST(request) {
  const jar = await cookies();
  const refreshToken = jar.get("refresh_token")?.value;

  if (!refreshToken) {
    return NextResponse.json({ message: "세션이 없습니다. 다시 로그인해주세요." }, { status: 401 });
  }

  // email 은 user_email 쿠키(30분 후 만료) -> 요청 본문 -> refresh_token 페이로드 순으로 확보
  let email = jar.get("user_email")?.value;
  if (!email) {
    try {
      email = (await request.json())?.email;
    } catch {
      /* 본문 없음 */
    }
  }
  if (!email) {
    try {
      email = jwtDecode(refreshToken)?.sub;
    } catch {
      /* 디코드 실패 */
    }
  }
  if (!email) {
    return NextResponse.json({ message: "세션 정보가 없습니다. 다시 로그인해주세요." }, { status: 401 });
  }

  try {
    const response = await fetch(backendUrl("/api/auth/reissue"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, refreshToken }),
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    const payload = await readBackendPayload(response);

    if (!response.ok || !payload?.accessToken) {
      const status = response.ok ? 502 : response.status;
      return NextResponse.json({ message: errorMessage(payload, "토큰 재발급에 실패했습니다.") }, { status });
    }

    const nextRefresh = payload.refreshToken ?? refreshToken;
    const result = NextResponse.json({ accessToken: payload.accessToken, refreshToken: nextRefresh });
    const base = { httpOnly: true, sameSite: "lax", secure: false, path: "/" };
    result.cookies.set("access_token", payload.accessToken, { ...base, maxAge: 60 * 30 });
    result.cookies.set("user_email", email, { ...base, maxAge: 60 * 30 });
    result.cookies.set("refresh_token", nextRefresh, { ...base, maxAge: 60 * 60 * 24 * 7 });
    return result;
  } catch {
    return NextResponse.json({ message: "인증 서버에 연결할 수 없습니다." }, { status: 503 });
  }
}
