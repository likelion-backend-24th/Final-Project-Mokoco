import { aiProxy } from "@/lib/ai-proxy";

export async function POST(request, { params }) {
  const { draftId } = await params;
  if (!/^\d+$/.test(draftId)) return Response.json({ message: "AI 초안 번호를 확인해주세요." }, { status: 400 });
  return aiProxy(request, `/api/ai/post-drafts/${draftId}/revisions`);
}
