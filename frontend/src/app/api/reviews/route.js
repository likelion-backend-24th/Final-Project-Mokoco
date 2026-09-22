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

// 프로덕션에서는 Caddy가 POST /api/reviews를 post-service로 직결시키므로(멀티파트 유실 회피,
// infra/Caddyfile 참고) 이 핸들러는 실제로 호출되지 않는다. Caddy 없이 `next dev`만 띄우는
// 로컬 환경에서의 폴백 — review-form.js가 보내는 review(JSON 파트) + images(파일 파트) 그대로 전달.
export async function POST(request) {
  const accessToken = (await cookies()).get("access_token")?.value;
  if (!accessToken) return Response.json({ message: "로그인이 필요합니다." }, { status: 401 });

  let incoming;
  try {
    incoming = await request.formData();
  } catch {
    return Response.json({ message: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  const outgoing = new FormData();
  const review = incoming.get("review");
  if (review) outgoing.append("review", review);
  for (const file of incoming.getAll("images")) {
    if (file instanceof File && file.size > 0) {
      outgoing.append("images", file, file.name);
    }
  }

  try {
    const response = await fetch(backendUrl("/reviews"), {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}` },
      body: outgoing,
      cache: "no-store",
      signal: AbortSignal.timeout(15000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ message: errorMessage(payload, "후기를 등록하지 못했습니다.") }, { status: response.status });
    }
    return Response.json({ success: true, reviewId: payload });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." }, { status: 502 });
  }
}
