import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function PATCH() {
  const accessToken = (await cookies()).get("access_token")?.value;
  if (!accessToken) return Response.json({ error: "로그인이 필요합니다." }, { status: 401 });
  try {
    const response = await fetch(backendUrl("/notifications/read-all"), {
      method: "PATCH",
      headers: { Authorization: `Bearer ${(await cookies()).get("access_token")?.value ?? ""}`,  },
      signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) return Response.json({ error: "읽음 처리에 실패했습니다." }, { status: response.status });
    return Response.json({ success: true });
  } catch {
    return Response.json({ error: "서버에 연결하지 못했습니다." }, { status: 502 });
  }
}
