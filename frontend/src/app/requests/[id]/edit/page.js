import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import RepairRequestForm from "@/components/repair-request-form";
import SiteHeader from "@/components/site-header";
import { backendUrl } from "@/lib/backend";

async function getPost(id) {
  try {
    const response = await fetch(backendUrl(`/posts/${id}`), { cache: "no-store", signal: AbortSignal.timeout(5000) });
    if (!response.ok) return null;
    return response.json();
  } catch {
    return null;
  }
}

export default async function EditRequestPage({ params }) {
  const { id } = await params;
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("mokoco_user_email")?.value;
  const accessToken = cookieStore.get("mokoco_access_token")?.value;

  if (!userEmail || !accessToken) redirect("/login");

  const post = await getPost(id);
  if (!post) notFound();
  if (post.authorEmail !== userEmail) redirect(`/requests/${id}`);

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="repair-form-page page-shell">
        <RepairRequestForm postId={id} initialValue={post} />
      </main>
    </div>
  );
}
