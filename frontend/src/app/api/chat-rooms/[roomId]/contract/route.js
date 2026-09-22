import { contractProxy } from "@/lib/contract-proxy";
export function GET(request, context) { return contractProxy(request, context, "GET"); }
export function POST(request, context) { return contractProxy(request, context, "POST"); }
