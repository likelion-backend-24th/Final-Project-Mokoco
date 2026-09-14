import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function aiProxy(request, path, multipart = false) {
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return Response.json({ message: "로그인이 필요합니다." }, { status: 401 });
  const origin = request.headers.get("origin");
  if (origin && origin !== new URL(request.url).origin) return Response.json({ message: "잘못된 요청 출처입니다." }, { status: 403 });
  const size = Number(request.headers.get("content-length") || 0);
  if (size > (multipart ? 16 * 1024 * 1024 : 128 * 1024)) return Response.json({ message: "입력 용량을 줄여주세요." }, { status: 413 });
  let body;
  try { body = multipart ? await request.formData() : JSON.stringify(await request.json()); }
  catch { return Response.json({ message: "입력 형식을 확인해주세요." }, { status: 400 }); }
  try {
    const response = await fetch(backendUrl(path), {
      method: "POST", headers: { Authorization: `Bearer ${token}`, ...(multipart ? {} : { "Content-Type": "application/json" }) },
      body, cache: "no-store", signal: AbortSignal.timeout(25000),
    });
    const payload = await response.json().catch(() => null);
    return Response.json(payload || { message: "AI 응답을 확인하지 못했습니다. 직접 작성해주세요." }, {
      status: payload ? response.status : 502, headers: { "Cache-Control": "no-store" },
    });
  } catch (failure) {
    return Response.json({ message: "AI 서버 연결이 지연되고 있습니다. 입력은 유지되므로 직접 작성하거나 다시 시도해주세요." },
      { status: failure.name === "TimeoutError" ? 504 : 502 });
  }
}
