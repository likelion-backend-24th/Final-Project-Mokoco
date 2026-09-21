import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function GET() {
  const userEmail = (await cookies()).get("user_email")?.value;
  if (!userEmail) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl("/notifications"), {
      headers: { Authorization: `Bearer ${(await cookies()).get("access_token")?.value ?? ""}`,  },
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) return Response.json({ error: "알림을 불러오지 못했습니다." }, { status: response.status });
    return Response.json(await response.json(), { headers: { "Cache-Control": "no-store" } });
  } catch {
    return Response.json({ error: "서버에 연결하지 못했습니다." }, { status: 502 });
  }
}
