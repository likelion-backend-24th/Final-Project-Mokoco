import { NextResponse } from "next/server";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

export async function POST(request) {
  try {
    const body = await request.json();
    const signupRequest = {
      name: body.name,
      nickname: body.nickname,
      email: body.email,
      password: body.password,
      regionCode: body.regionCode ?? null,
    };
    const response = await fetch(backendUrl("/api/auth/signup"), { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(signupRequest), cache: "no-store", signal: AbortSignal.timeout(5000) });
    const payload = await readBackendPayload(response);
    if (!response.ok) return NextResponse.json({ message: errorMessage(payload, "회원가입에 실패했습니다.") }, { status: response.status });
    return NextResponse.json({ userId: payload }, { status: 201 });
  } catch { return NextResponse.json({ message: "회원가입 서버에 연결할 수 없습니다." }, { status: 503 }); }
}
