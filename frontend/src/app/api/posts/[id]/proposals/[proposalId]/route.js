import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function DELETE(request, { params }) {
  try {
    const resolvedParams = await params;
    const postId = resolvedParams.postId || resolvedParams.id;
    const proposalId = resolvedParams.proposalId;
    
    const cookieStore = await cookies();
    const userEmail = cookieStore.get("user_email")?.value;

    if (!userEmail) {
      return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
    }

    const targetUrl = backendUrl(`/posts/${postId}/proposals/${proposalId}`);

    const response = await fetch(targetUrl, {
      method: "DELETE",
      headers: {
        "X-User-Email": userEmail,
      },
    });

    if (!response.ok) {
      const errorText = await response.text();
      return Response.json({ error: "제안 삭제 실패", details: errorText }, { status: response.status });
    }

    return Response.json({ success: true });
  } catch (error) {
    return Response.json({ error: "서버 연결 실패", message: error.message }, { status: 500 });
  }
}