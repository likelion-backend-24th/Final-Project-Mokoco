import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function GET(request) {
  const { searchParams } = new URL(request.url);
  const revieweeEmail = searchParams.get("revieweeEmail");
  if (!revieweeEmail) return Response.json({ error: "revieweeEmail이 필요합니다." }, { status: 400 });

  const page = searchParams.get("page") ?? "0";
  const size = searchParams.get("size") ?? "10";

  try {
    const response = await fetch(
      backendUrl(`/reviews?revieweeEmail=${encodeURIComponent(revieweeEmail)}&page=${page}&size=${size}`),
      { cache: "no-store", signal: AbortSignal.timeout(8000) },
    );
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "후기를 불러오지 못했습니다.") }, { status: response.status });
    }
    return Response.json(payload);
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}

export async function POST(request) {
  const email = (await cookies()).get("user_email")?.value;
  if (!email) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  let incoming;
  try {
    incoming = await request.formData();
  } catch {
    return Response.json({ error: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  const postId = incoming.get("postId");
  const rating = incoming.get("rating");
  const content = incoming.get("content");

  // 백엔드는 review(JSON 파트) + images(파일 파트들)로 구성된 멀티파트를 기대한다.
  const outgoing = new FormData();
  outgoing.append(
    "review",
    new Blob([JSON.stringify({ postId: Number(postId), rating: Number(rating), content })], {
      type: "application/json",
    }),
  );
  for (const file of incoming.getAll("images")) {
    if (file instanceof File && file.size > 0) {
      outgoing.append("images", file, file.name);
    }
  }

  try {
    const response = await fetch(backendUrl("/reviews"), {
      method: "POST",
      headers: { "X-User-Email": email },
      body: outgoing,
      cache: "no-store",
      signal: AbortSignal.timeout(15000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "후기를 등록하지 못했습니다."), code: payload?.code }, { status: response.status });
    }
    return Response.json({ success: true, reviewId: payload });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." }, { status: 502 });
  }
}