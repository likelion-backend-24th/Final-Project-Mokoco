import { NextResponse } from "next/server";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

export async function POST(request) {
  try {
    const body = await request.json();
    const response = await fetch(backendUrl("/api/auth/signin"), { 
      method: "POST", 
      headers: { "Content-Type": "application/json" }, 
      body: JSON.stringify({ email: body.email, password: body.password }), 
      cache: "no-store", 
      signal: AbortSignal.timeout(5000) 
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) return NextResponse.json({ message: errorMessage(payload, "로그인에 실패했습니다.") }, { status: response.status });
    if (!payload?.accessToken || !payload?.refreshToken) {
      return NextResponse.json({ message: "인증 서버 응답 형식이 올바르지 않습니다." }, { status: 502 });
    }

    // 클라이언트 스토어에서도 쓸 수 있도록 accessToken, refreshToken을 바디에 포함해서 리턴
    const result = NextResponse.json({ 
      success: true, 
      accessToken: payload.accessToken, 
      refreshToken: payload.refreshToken 
    });

    // const secure = process.env.NODE_ENV === "production";
    // 로컬 HTTP 환경 테스트를 위해 secure를 일단 강제로 false로 설정
    const secure = false;
    result.cookies.set("access_token", payload.accessToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 60 * 30 });
    result.cookies.set("refresh_token", payload.refreshToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 60 * 60 * 24 * 7 });
    result.cookies.set("user_email", body.email, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 60 * 30 });
    return result;
  } catch { 
    return NextResponse.json({ message: "인증 서버에 연결할 수 없습니다." }, { status: 503 }); 
  }
}