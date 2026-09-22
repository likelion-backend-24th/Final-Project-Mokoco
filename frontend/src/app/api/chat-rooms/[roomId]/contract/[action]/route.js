import { contractProxy } from "@/lib/contract-proxy";
export function POST(request, context) { return contractProxy(request, context, "POST"); }
