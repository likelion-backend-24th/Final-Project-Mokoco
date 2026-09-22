import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function POST(request, { params }) {
  try {
    const resolvedParams = await params;
    // [id] 폴더명과 [postId] 모두 대응 가능하도록 처리
    const postId = resolvedParams.postId || resolvedParams.id;
    const body = await request.json();
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
    }

    const targetUrl = backendUrl(`/posts/${postId}/proposals`);

    const response = await fetch(targetUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const payload = await readBackendPayload(response);
      return Response.json({ error: errorMessage(payload, "수리 제안을 등록하지 못했습니다.") }, { status: response.status });
    }

    const text = await response.text();
    const proposalId = text ? JSON.parse(text) : null;
    return Response.json({ proposalId });
  } catch (error) {
    return Response.json({ error: "서버 연결 실패", message: error.message }, { status: 500 });
  }
}

