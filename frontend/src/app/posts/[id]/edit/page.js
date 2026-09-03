import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import PostForm from "@/components/post-form";
import SiteHeader from "@/components/site-header";
import { backendUrl } from "@/lib/backend";

// accessToken을 인자로 받아 헤더에 포함시킵니다.
async function getPost(id, accessToken) {
  try {
    const response = await fetch(backendUrl(`/posts/${id}`), {
      cache: "no-store",
      headers: {
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) return null;
    return response.json();
  } catch {
    return null;
  }
}

export default async function EditPostPage({ params }) {
  const { id } = await params;
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value;
  const accessToken = cookieStore.get("access_token")?.value;

  if (!userEmail || !accessToken) redirect("/login");

  // accessToken을 함께 전달
  const post = await getPost(id, accessToken);
  if (!post) notFound();
  if (post.authorEmail !== userEmail) redirect(`/posts/${id}`);

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="repair-form-page page-shell">
        <PostForm postId={id} initialValue={post} />
      </main>
    </div>
  );
}