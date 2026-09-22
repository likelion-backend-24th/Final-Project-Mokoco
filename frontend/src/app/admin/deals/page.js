import { cookies } from "next/headers";
import Link from "next/link";
import { ShieldWarning } from "@phosphor-icons/react/dist/ssr";
import AdminDealTable from "@/components/admin-deal-table";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export const dynamic = "force-dynamic";

async function getAdminDeals(token) {
  if (!token) return { data: null, error: "로그인이 필요합니다." };
  try {
    const response = await fetch(backendUrl("/api/admin/deals?page=0&size=20"), {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return { data: null, error: errorMessage(payload, "관리자만 접근할 수 있습니다.") };
    }
    return { data: payload, error: null };
  } catch {
    return { data: null, error: "서버에 연결할 수 없습니다." };
  }
}

export default async function AdminDealsPage() {
  const token = (await cookies()).get("access_token")?.value ?? null;
  const { data: deals, error } = await getAdminDeals(token);

  if (error || !deals) {
    return (
      <div className="reference-empty-state">
        <ShieldWarning size={58} weight="duotone" />
        <h3>접근할 수 없어요</h3>
        <p>{error ?? "관리자 계정으로 로그인해주세요."}</p>
        <Link href="/" className="compact-primary-button">홈으로</Link>
      </div>
    );
  }

  return (
    <section>
      <div className="section-heading">
        <div>
          <p className="section-kicker">ADMIN</p>
          <h2>거래 현황</h2>
        </div>
      </div>
      <AdminDealTable initialPage={deals} />
    </section>
  );
}
