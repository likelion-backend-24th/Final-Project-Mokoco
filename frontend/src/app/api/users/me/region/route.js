import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { backendUrl, errorMessage, readBackendPayload } from "@/lib/backend";

async function forwardRegionPost(method, body) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("access_token")?.value;

  if (!accessToken) {
    return NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  }

  try {
    const response = await fetch(backendUrl("/api/users/me/region"), {
      method,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      ...(body ? { body: JSON.stringify(body) } : {}),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const payload = await readBackendPayload(response);

    if (!response.ok) {
      return NextResponse.json(
        { message: errorMessage(payload, "지역 정보를 처리하지 못했습니다.") },
        { status: response.status },
      );
    }

    return NextResponse.json(payload);
  } catch {
    return NextResponse.json({ message: "지역 정보 서버에 연결할 수 없습니다." }, { status: 503 });
  }
}

export function GET() {
  return forwardRegionPost("GET");
}

export async function PATCH(post) {
  try {
    const body = await post.json();
    return forwardRegionPost("PATCH", {
      latitude: body.latitude,
      longitude: body.longitude,
    });
  } catch {
    return NextResponse.json({ message: "위치 정보 형식이 올바르지 않습니다." }, { status: 400 });
  }
}
