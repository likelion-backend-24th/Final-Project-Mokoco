import { cookies } from "next/headers";
import Link from "next/link";
import { ShieldWarning, Users, UserPlus, Receipt, Flag, CurrencyCircleDollar, HandCoins } from "@phosphor-icons/react/dist/ssr";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export const dynamic = "force-dynamic";

async function getAdminJson(token, path, fallbackMessage) {
  if (!token) return { data: null, error: "로그인이 필요합니다." };
  try {
    const response = await fetch(backendUrl(path), {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
    const payload = await readBackendPayload(response);
    if (!response.ok) {
      return { data: null, error: errorMessage(payload, fallbackMessage) };
    }
    return { data: payload, error: null };
  } catch {
    return { data: null, error: "서버에 연결할 수 없습니다." };
  }
}

function won(amount) {
  return `${(amount ?? 0).toLocaleString("ko-KR")}원`;
}

function StatCard({ icon: Icon, label, value, tone, href }) {
  const toneClass = {
    blue: "border-blue-200 bg-blue-50 text-blue-700",
    emerald: "border-emerald-200 bg-emerald-50 text-emerald-700",
    amber: "border-amber-200 bg-amber-50 text-amber-700",
    slate: "border-slate-200 bg-slate-50 text-slate-700",
  }[tone ?? "slate"];

  return (
    <Link
      href={href}
      className={`block rounded-xl border px-4 py-3.5 transition-shadow hover:shadow-md ${toneClass}`}
    >
      <div className="flex items-center gap-1.5 text-xs font-semibold opacity-80">
        <Icon size={14} weight="bold" />
        {label}
      </div>
      <p className="mt-1.5 text-xl font-extrabold">{value}</p>
    </Link>
  );
}

export default async function AdminOverviewPage() {
  const token = (await cookies()).get("access_token")?.value ?? null;
  const [{ data: overview, error: overviewError }, { data: paymentSummary }] = await Promise.all([
    getAdminJson(token, "/api/admin/overview", "관리자만 접근할 수 있습니다."),
    getAdminJson(token, "/api/admin/deals/summary", "요약 정보를 불러오지 못했습니다."),
  ]);

  if (overviewError || !overview) {
    return (
      <div className="reference-empty-state">
        <ShieldWarning size={58} weight="duotone" />
        <h3>접근할 수 없어요</h3>
        <p>{overviewError ?? "관리자 계정으로 로그인해주세요."}</p>
        <Link href="/" className="compact-primary-button">홈으로</Link>
      </div>
    );
  }

  return (
    <section>
      <div className="section-heading">
        <div>
          <p className="section-kicker">ADMIN</p>
          <h2>대시보드 개요</h2>
        </div>
      </div>

      <div className="dashboard-card">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          <StatCard icon={Users} label="총 회원 수" value={`${overview.totalUsers.toLocaleString("ko-KR")}명`} tone="blue" href="/admin/users" />
          <StatCard icon={UserPlus} label="오늘 가입자" value={`${overview.newUsersToday.toLocaleString("ko-KR")}명`} tone="blue" href="/admin/users" />
          <StatCard icon={Receipt} label="진행 중인 거래" value={`${overview.activeDeals.toLocaleString("ko-KR")}건`} tone="amber" href="/admin/deals" />
          <StatCard icon={Flag} label="신고 대기" value={`${overview.pendingReports.toLocaleString("ko-KR")}건`} tone="amber" href="/admin/reports" />
          <StatCard icon={CurrencyCircleDollar} label="거래 완료 금액" value={won(paymentSummary?.totalCompletedAmount)} tone="emerald" href="/admin/deals" />
          <StatCard icon={HandCoins} label="정산된 금액" value={won(paymentSummary?.totalSettledAmount)} tone="emerald" href="/admin/payments" />
        </div>
      </div>
    </section>
  );
}
