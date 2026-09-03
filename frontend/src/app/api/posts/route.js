import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

export async function POST(request) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("mokoco_access_token")?.value;
  const userEmail = cookieStore.get("mokoco_user_email")?.value;

  if (!accessToken || !userEmail) {
    return NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  }

  try {
    const body = await request.json();
    const title = typeof body.title === "string" ? body.title.trim() : "";
    const content = typeof body.content === "string" ? body.content.trim() : "";

    if (!title || !content) {
      return NextResponse.json({ message: "제목과 요청 내용을 모두 입력해주세요." }, { status: 400 });
    }

    const response = await fetch(backendUrl("/posts"), {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
        "X-User-Email": userEmail,
      },
      body: JSON.stringify({ title, content }),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);

    if (!response.ok) {
      return NextResponse.json(
        { message: errorMessage(payload, "수리 요청을 등록하지 못했습니다.") },
        { status: response.status },
      );
    }

    return NextResponse.json({ id: payload }, { status: 201 });
  } catch {
    return NextResponse.json({ message: "수리 요청 서버에 연결할 수 없습니다." }, { status: 503 });
  }
}
