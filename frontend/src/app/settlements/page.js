import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import SiteHeader from "@/components/site-header";
import SettlementsView from "@/components/settlements-view";

export default async function SettlementsPage() {
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;

  if (!userEmail) {
    redirect("/login");
  }

  return (
    <>
      <SiteHeader userEmail={userEmail} />
      <SettlementsView />
    </>
  );
}
