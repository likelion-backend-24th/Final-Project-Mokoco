import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import SiteHeader from "@/components/site-header";
import PostForm from "@/components/post-form";

export default async function NewRepairPostPage() {
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value;
  const accessToken = cookieStore.get("access_token")?.value;

  if (!userEmail || !accessToken) redirect("/login");

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="repair-form-page page-shell">
        <PostForm userEmail={userEmail} accessToken={accessToken} />
      </main>
    </div>
  );
}