// frontend/src/app/profile/page.js
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import SiteHeader from "@/components/site-header";
import ProfileTabs from "@/components/profile-tabs";

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
        <ProfileTabs userEmail={userEmail} />
      </main>
    </div>
  );
}