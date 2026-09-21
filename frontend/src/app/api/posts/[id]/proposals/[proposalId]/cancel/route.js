import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function PATCH(request, { params }) {
  try {
    const resolvedParams = await params;
    const postId = resolvedParams.postId || resolvedParams.id;
    const proposalId = resolvedParams.proposalId;

    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
    }

    const targetUrl = backendUrl(`/posts/${postId}/proposals/${proposalId}/cancel`);

    const response = await fetch(targetUrl, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${(await cookies()).get("access_token")?.value ?? ""}`,

      },
    });

    if (!response.ok) {
      const errorText = await response.text();
      return Response.json({ error: "제안 채택취소 실패", details: errorText }, { status: response.status });
    }

    return Response.json({ success: true });
  } catch (error) {
    return Response.json({ error: "서버 연결 실패", message: error.message }, { status: 500 });
  }
}
