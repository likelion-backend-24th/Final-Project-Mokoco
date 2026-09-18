import { cookies } from "next/headers";
import { redirect, notFound } from "next/navigation";
import SiteHeader from "@/components/site-header";
import RepairContract from "@/components/repair-contract";
export default async function ContractPage({ params }) {
  const { roomId } = await params;
  if (!/^\d+$/.test(roomId)) notFound();
  const cookieStore = await cookies();
  if (!cookieStore.get("access_token")?.value) redirect("/login");
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <RepairContract roomId={roomId} />
    </div>
  );
}
