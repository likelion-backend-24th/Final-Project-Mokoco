import { cookies } from "next/headers";
import Link from "next/link";
import { ShieldWarning } from "@phosphor-icons/react/dist/ssr";
import AdminReportTable from "@/components/admin-report-table";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export const dynamic = "force-dynamic";

async function getAdminData(token, path) {
  if (!token) return { data: null, error: "로그인이 필요합니다." };
  try {
    const response = await fetch(backendUrl(path), {
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

export default async function AdminReportsPage() {
  const cookieStore = await cookies();
  const token = cookieStore.get("access_token")?.value ?? null;
  // 신고 접수함은 회원 목록으로 이미 관리자 확인이 끝난 뒤 보여주던 화면이었지만,
  // 사이드바 구조에서는 이 경로에 바로 들어올 수 있으니 여기서도 독립적으로 관리자 여부를 확인한다.
  const { data: reports, error } = await getAdminData(token, "/api/admin/reports");

  if (error) {
    return (
      <div className="reference-empty-state">
        <ShieldWarning size={58} weight="duotone" />
        <h3>접근할 수 없어요</h3>
        <p>{error}</p>
        <Link href="/" className="compact-primary-button">홈으로</Link>
      </div>
    );
  }

  return (
    <section>
      <div className="section-heading">
        <div>
          <p className="section-kicker">ADMIN</p>
          <h2>신고 접수함</h2>
        </div>
      </div>
      <AdminReportTable initialReports={reports ?? []} />
    </section>
  );
}
