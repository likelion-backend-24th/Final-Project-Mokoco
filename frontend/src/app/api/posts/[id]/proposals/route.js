import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function POST(request, { params }) {
  try {
    const resolvedParams = await params;
    // [id] 폴더명과 [postId] 모두 대응 가능하도록 처리
    const postId = resolvedParams.postId || resolvedParams.id;
    const body = await request.json();
    const cookieStore = await cookies();
    const userEmail = cookieStore.get("user_email")?.value;

    if (!userEmail) {
      return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
    }

    const targetUrl = backendUrl(`/posts/${postId}/proposals`);

    const response = await fetch(targetUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-User-Email": userEmail,
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const errorText = await response.text();
      return Response.json({ error: "백엔드 제안 등록 실패", details: errorText }, { status: response.status });
    }

    const text = await response.text();
    const proposalId = text ? JSON.parse(text) : null;
    return Response.json({ proposalId });
  } catch (error) {
    return Response.json({ error: "서버 연결 실패", message: error.message }, { status: 500 });
  }
}

