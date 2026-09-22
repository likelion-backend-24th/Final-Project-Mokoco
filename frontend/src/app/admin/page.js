import { cookies } from "next/headers";
import Link from "next/link";
import { ShieldWarning } from "@phosphor-icons/react/dist/ssr";
import SiteHeader from "@/components/site-header";
import AdminUserTable from "@/components/admin-user-table";
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

export default async function AdminPage() {
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  const token = cookieStore.get("access_token")?.value ?? null;
  const { data: users, error } = await getAdminData(token, "/api/admin/users");
  // 유저 목록이 이미 관리자 여부를 확인해주니, 신고 목록은 실패해도(거의 없겠지만) 페이지 전체를 막지 않는다.
  const { data: reports } = users ? await getAdminData(token, "/api/admin/reports") : { data: null };

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="page-shell auth-main">
        {error || !users ? (
          <div className="reference-empty-state">
            <ShieldWarning size={58} weight="duotone" />
            <h3>접근할 수 없어요</h3>
            <p>{error ?? "관리자 계정으로 로그인해주세요."}</p>
            <Link href="/" className="compact-primary-button">홈으로</Link>
          </div>
        ) : (
          <div className="space-y-10">
            <section>
              <div className="section-heading">
                <div>
                  <p className="section-kicker">ADMIN</p>
                  <h2>회원 관리</h2>
                </div>
              </div>
              <AdminUserTable initialUsers={users} currentUserEmail={userEmail} />
            </section>

            <section>
              <div className="section-heading">
                <div>
                  <p className="section-kicker">ADMIN</p>
                  <h2>신고 접수함</h2>
                </div>
              </div>
              <AdminReportTable initialReports={reports ?? []} />
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
