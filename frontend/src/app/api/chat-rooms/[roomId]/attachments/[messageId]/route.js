import { cookies } from "next/headers";
import { backendUrl } from "@/lib/backend";

export async function GET(request, { params }) {
  const { roomId, messageId } = await params;
  const token = (await cookies()).get("access_token")?.value;
  if (!token) return new Response(null, { status: 401 });
  if (!/^\d+$/.test(roomId) || !/^\d+$/.test(messageId)) return new Response(null, { status: 400 });
  try {
    const headers = { Authorization: `Bearer ${token}` };
    if (request.headers.has("range")) headers.Range = request.headers.get("range");
    const response = await fetch(backendUrl(`/api/chat-rooms/${roomId}/attachments/${messageId}`), { headers, cache: "no-store", signal: request.signal });
    const output = new Headers({ "Cache-Control": "private, no-store", "X-Content-Type-Options": "nosniff" });
    for (const name of ["content-type", "content-length", "content-range", "accept-ranges", "content-disposition"])
      if (response.headers.has(name)) output.set(name, response.headers.get(name));
    return new Response(response.body, { status: response.status, headers: output });
  } catch { return new Response(null, { status: 502 }); }
}
