import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function PATCH(request, { params }) {
  try {
    const resolvedParams = await params;
    const postId = resolvedParams.postId || resolvedParams.id;
    const proposalId = resolvedParams.proposalId;

    const cookieStore = await cookies();
    const userEmail = cookieStore.get("user_email")?.value;

    if (!userEmail) {
      return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
    }

    const response = await fetch(backendUrl(`/posts/${postId}/proposals/${proposalId}/cancel`), {
      method: "PATCH",
      headers: { "X-User-Email": userEmail },
    });

    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "채택을 취소하지 못했습니다.") }, { status: response.status });
    }

    return Response.json({ success: true });
  } catch (error) {
    return Response.json({ error: "서버 연결 실패", message: error.message }, { status: 500 });
  }
}
