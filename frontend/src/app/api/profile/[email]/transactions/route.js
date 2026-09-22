import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function GET(request, { params }) {
  const { email } = await params;
  const { searchParams } = new URL(request.url);
  const role = searchParams.get("role") ?? "repairer";
  const page = searchParams.get("page") ?? "0";
  const size = searchParams.get("size") ?? "10";

  try {
    const response = await fetch(
      backendUrl(`/profile/${encodeURIComponent(email)}/transactions?role=${role}&page=${page}&size=${size}`),
      { cache: "no-store", signal: AbortSignal.timeout(8000) },
    );
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return Response.json({ error: errorMessage(payload, "거래 내역을 불러오지 못했습니다.") }, { status: response.status });
    }
    return Response.json(payload);
  } catch {
    return Response.json({ error: "서버에 연결할 수 없습니다." }, { status: 502 });
  }
}
