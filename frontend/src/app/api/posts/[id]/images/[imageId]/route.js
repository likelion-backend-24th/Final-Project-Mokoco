import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

export async function DELETE(_request, { params }) {
  const { id, imageId } = await params;
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("access_token")?.value;
  const userEmail = cookieStore.get("user_email")?.value;

  if (!accessToken || !userEmail) {
    return NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  }

  try {
    const response = await fetch(backendUrl(`/posts/${id}/images/${imageId}`), {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${accessToken}`,

      },
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });

    if (response.ok) {
      return NextResponse.json({ success: true });
    }

    const payload = await readBackendPayload(response);
    return NextResponse.json(
      { message: errorMessage(payload, "사진을 삭제하지 못했습니다.") },
      { status: response.status },
    );
  } catch {
    return NextResponse.json({ message: "수리 요청 서버에 연결할 수 없습니다." }, { status: 503 });
  }
}
