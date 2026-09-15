import { aiProxy } from "@/lib/ai-proxy";
export async function POST(request) { return aiProxy(request, "/api/ai/post-draft", true); }
