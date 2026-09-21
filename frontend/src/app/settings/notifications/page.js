import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import SiteHeader from "@/components/site-header";
import NotificationSettingsView from "@/components/notification-settings-view";

export default async function NotificationSettingsPage() {
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;

  if (!userEmail) {
    redirect("/login");
  }

  return (
    <>
      <SiteHeader userEmail={userEmail} />
      <NotificationSettingsView />
    </>
  );
}
