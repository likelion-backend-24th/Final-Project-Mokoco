import { cookies } from "next/headers";
import Link from "next/link";
import { ShieldWarning } from "@phosphor-icons/react/dist/ssr";
import AdminUserTable from "@/components/admin-user-table";
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

export default async function AdminUsersPage() {
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  const token = cookieStore.get("access_token")?.value ?? null;
  const { data: users, error } = await getAdminData(token, "/api/admin/users");

  if (error || !users) {
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
          <h2>회원 관리</h2>
        </div>
      </div>
      <AdminUserTable initialUsers={users} currentUserEmail={userEmail} />
    </section>
  );
}
