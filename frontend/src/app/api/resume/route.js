import { cookies } from "next/headers";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

async function requireAccessToken() {
  return (await cookies()).get("access_token")?.value ?? null;
}

export async function GET() {
  const accessToken = await requireAccessToken();
  if (!accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  try {
    const response = await fetch(backendUrl("/resumes/me"), {
      headers: { Authorization: `Bearer ${accessToken}` },
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "이력서를 불러오지 못했습니다.") }, { status: response.status });
    }
    return Response.json(payload);
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}

export async function POST(request) {
  const accessToken = await requireAccessToken();
  if (!accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  try {
    const response = await fetch(backendUrl("/resumes"), {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        headline: body.headline,
        introduction: body.introduction,
        skills: body.skills,
        careers: body.careers,
      }),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "이력서를 작성하지 못했습니다.") }, { status: response.status });
    }
    return Response.json({ success: true });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}

export async function PATCH(request) {
  const accessToken = await requireAccessToken();
  if (!accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  try {
    const response = await fetch(backendUrl("/resumes"), {
      method: "PATCH",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        headline: body.headline,
        introduction: body.introduction,
        skills: body.skills,
        careers: body.careers,
      }),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "이력서를 수정하지 못했습니다.") }, { status: response.status });
    }
    return Response.json({ success: true });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}

export async function DELETE() {
  const accessToken = await requireAccessToken();
  if (!accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });

  try {
    const response = await fetch(backendUrl("/resumes"), {
      method: "DELETE",
      headers: { Authorization: `Bearer ${accessToken}` },
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "이력서를 삭제하지 못했습니다.") }, { status: response.status });
    }
    return Response.json({ success: true });
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
