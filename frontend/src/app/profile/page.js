import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import SiteHeader from "@/components/site-header";
import ProfileTabs from "@/components/profile-tabs";
import ResumeEditor from "@/components/resume-editor";
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
        <h2 className="mt-1 text-2xl font-extrabold tracking-[-0.03em] text-slate-950">내 프로필</h2>
        <p className="mt-1 mb-6 text-sm text-slate-500">참여했던 거래와 후기를 확인할 수 있어요.</p>

        <section className="mb-8">
          <h3 className="mb-2 text-sm font-bold text-slate-700">내 정보</h3>
          <AccountSettings />
        </section>

        <section className="mb-8">
          <h3 className="mb-2 text-sm font-bold text-slate-700">내 이력서</h3>
          <ResumeEditor />
        </section>

        <ProfileTabs userEmail={userEmail} />
      </main>
    </div>
  );
}