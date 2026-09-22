import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import SiteHeader from "@/components/site-header";
import ProfileTabs from "@/components/profile-tabs";
import AccountSettings from "@/components/account-settings";

export default async function ProfilePage() {
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;

  if (!userEmail) {
    redirect("/login");
  }

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="page-shell auth-main">
        <p className="section-kicker">PROFILE</p>
        <h2 className="mt-1 text-3xl font-extrabold tracking-[-0.03em] text-slate-950">내 프로필</h2>
        <p className="mt-2 mb-8 text-base text-slate-500">참여했던 거래와 후기를 확인할 수 있어요.</p>

        <section className="mb-10">
          <h3 className="mb-3 text-base font-bold text-slate-700">내 정보</h3>
          <AccountSettings />
        </section>

        <section className="mb-10 flex items-center justify-between gap-4 rounded-xl border border-slate-200 bg-white px-5 py-4">
          <div>
            <h3 className="text-base font-bold text-slate-700">정산 내역</h3>
            <p className="mt-0.5 text-sm text-slate-500">수리자로 참여해 받은 결제 내역을 확인할 수 있어요.</p>
          </div>
          <Link
            href="/settlements"
            className="shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
          >
            정산 내역 보기
          </Link>
        </section>

        <ProfileTabs userEmail={userEmail} />
      </main>
    </div>
  );
}
