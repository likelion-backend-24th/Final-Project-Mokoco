import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

async function forwardPost(id, method, body) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("access_token")?.value;
  const userEmail = cookieStore.get("user_email")?.value;

  if (!accessToken || !userEmail) {
    return NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  }

  try {
    const response = await fetch(backendUrl(`/posts/${id}`), {
      method,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      ...(body ? { body: JSON.stringify(body) } : {}),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });

    if (response.status === 204) {
      return NextResponse.json({ success: true });
    }

    const payload = await readBackendPayload(response);

    if (!response.ok) {
      return NextResponse.json(
        { message: errorMessage(payload, "요청을 처리하지 못했습니다.") },
        { status: response.status },
      );
    }

    return NextResponse.json(payload);
  } catch {
    return NextResponse.json({ message: "수리 요청 서버에 연결할 수 없습니다." }, { status: 503 });
  }
}

export async function PATCH(request, { params }) {
  const { id } = await params;
  let body;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ message: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  const title = typeof body.title === "string" ? body.title.trim() : "";
  const content = typeof body.content === "string" ? body.content.trim() : "";
  const category = typeof body.category === "string" ? body.category : "";
  const contentFormat = body.contentFormat === "HTML" ? "HTML" : "PLAIN_TEXT";
  if (!title || !content || !category) {
    return NextResponse.json({ message: "제목, 요청 내용과 카테고리를 모두 입력해주세요." }, { status: 400 });
  }

  if (typeof body.category === "string" && body.category) payload.category = body.category;
  return forwardPost(id, "PATCH", { title, content, category, contentFormat });
}

export async function DELETE(_post, { params }) {
  const { id } = await params;
  return forwardPost(id, "DELETE");
}