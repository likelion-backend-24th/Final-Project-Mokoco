import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import SiteHeader from "@/components/site-header";
import RepairRequestForm from "@/components/repair-request-form";

export default async function NewRepairRequestPage() {
  const cookieStore = await cookies();
  // mokoco_ 접두사 제거
  const userEmail = cookieStore.get("user_email")?.value;
  const accessToken = cookieStore.get("access_token")?.value;

  if (!userEmail || !accessToken) redirect("/login");

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="repair-form-page page-shell">
        <RepairRequestForm />
      </main>
    </div>
  );
}