import { cookies } from "next/headers";
import SiteHeader from "@/components/site-header";
import AdminSidebar from "@/components/admin-sidebar";

export default async function AdminLayout({ children }) {
  const userEmail = (await cookies()).get("user_email")?.value ?? null;

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="page-shell auth-main flex gap-8">
        <AdminSidebar />
        <div className="min-w-0 flex-1">{children}</div>
      </main>
    </div>
  );
}
