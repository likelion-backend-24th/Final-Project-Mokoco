import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

export async function POST(request, { params }) {
  const { id } = await params;
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("access_token")?.value;
  const userEmail = cookieStore.get("user_email")?.value;

  if (!accessToken) {
    return NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  }

  try {
    const incoming = await request.formData();
    const outgoing = new FormData();
    let imageCount = 0;

    for (const image of incoming.getAll("images")) {
      if (image instanceof File && image.size > 0) {
        outgoing.append("images", image, image.name);
        imageCount += 1;
      }
    }

    if (imageCount === 0) {
      return NextResponse.json({ message: "추가할 사진을 선택해주세요." }, { status: 400 });
    }

    const response = await fetch(backendUrl(`/posts/${id}/images`), {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,

      },
      body: outgoing,
      cache: "no-store",
      signal: AbortSignal.timeout(15000),
    });
    const payload = await readBackendPayload(response);

    if (!response.ok) {
      return NextResponse.json(
        { message: errorMessage(payload, "사진을 추가하지 못했습니다.") },
        { status: response.status },
      );
    }

    return NextResponse.json(payload);
  } catch {
    return NextResponse.json({ message: "수리 요청 서버에 연결할 수 없습니다." }, { status: 503 });
  }
}
