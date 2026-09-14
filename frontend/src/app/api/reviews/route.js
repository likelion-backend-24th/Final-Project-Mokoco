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

  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  try {
    const response = await fetch(backendUrl("/reviews"), {
      method: "POST",
      headers: {
        "X-User-Email": email,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        postId: body.postId,
        rating: body.rating,
        content: body.content,
      }),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
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
