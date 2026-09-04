import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

export async function POST(request) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("access_token")?.value;
  const userEmail = cookieStore.get("user_email")?.value;

  if (!accessToken || !userEmail) {
    return NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  }

  try {
    // 프론트엔드에서 보낸 FormData 그대로 받기
    const formData = await request.formData();

    // 백엔드로 그대로 전달하기 위해 새로운 FormData 구성
    const backendFormData = new FormData();
    
    const postBlob = formData.get("post");
    if (postBlob) {
      backendFormData.append("post", postBlob);
    }

    const images = formData.getAll("images");
    images.forEach((image) => {
      backendFormData.append("images", image);
    });

    const response = await fetch(backendUrl("/posts"), {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "X-User-Email": userEmail,
        // 주의: multipart/form-data 전송 시 Content-Type 헤더는 수동으로 넣지 않아야 boundary가 자동 생성됩니다.
      },
      body: backendFormData,
      cache: "no-store",
      signal: AbortSignal.timeout(15000), // 파일 업로드가 있으니 타임아웃 넉넉히
    });

    const payload = await readBackendPayload(response);

    if (!response.ok) {
      return NextResponse.json(
        { message: errorMessage(payload, "수리 요청을 등록하지 못했습니다.") },
        { status: response.status },
      );
    }

    return NextResponse.json(payload, { status: 201 });
  } catch (error) {
    console.error("Post creation error:", error);
    return NextResponse.json({ message: "수리 요청 서버에 연결할 수 없습니다." }, { status: 503 });
  }
}