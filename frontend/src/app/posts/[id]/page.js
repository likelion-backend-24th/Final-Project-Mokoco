import Link from "next/link";
import { cookies } from "next/headers";
import { notFound } from "next/navigation";
import { ArrowLeft, CheckCircle, Wrench } from "@phosphor-icons/react/dist/ssr";
import SiteHeader from "@/components/site-header";
import PostActions from "@/components/post-actions";
import RepairProposalForm from "@/components/proposal-form";
import ProposalList from "@/components/proposal-list";
import { backendUrl } from "@/lib/backend";

const statusLabel = { WAITING: "도움 기다리는 중", MATCHED: "이웃과 연결됨", COMPLETED: "수리 완료" };

async function getPost(id) {
  try {
    const response = await fetch(backendUrl(`/posts/${id}`), { cache: "no-store", signal: AbortSignal.timeout(5000) });
    if (response.status === 404) return { post: null, error: null };
    if (!response.ok) return { post: null, error: "수리 요청을 불러오지 못했습니다." };
    return { post: await response.json(), error: null };
  } catch {
    return { post: null, error: "백엔드 서버에 연결할 수 없습니다. 서버 실행 상태를 확인해주세요." };
  }
}

async function getProposals(id) {
  try {
    const response = await fetch(backendUrl(`/posts/${id}/proposals`), { cache: "no-store" });
    if (!response.ok) return [];
    return await response.json();
  } catch {
    return [];
  }
}

function formatDate(value) {
  if (!value) return "시간 정보 없음";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "시간 정보 없음";
  return date.toLocaleString("ko-KR", { dateStyle: "medium", timeStyle: "short" });
}

export default async function PostDetailPage({ params }) {
  const { id } = await params;
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  
  const [{ post, error }, proposals] = await Promise.all([
    getPost(id),
    getProposals(id),
  ]);

  if (!post && !error) {
    notFound();
  }

  const isMine = post && userEmail && post.authorEmail === userEmail;

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="page-shell auth-main max-w-[760px]">
        <Link href="/posts" className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-slate-500 hover:text-blue-600">
          <ArrowLeft size={16} weight="bold" />
          목록으로
        </Link>

        {error ? (
          <div className="reference-empty-state">
            <h3>데이터 연결을 확인해주세요</h3>
            <p>{error}</p>
          </div>
        ) : (
          <div className="space-y-6">
            <div className="dashboard-card">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <span className={`status-badge status-${post.status?.toLowerCase()}`}>
                    {statusLabel[post.status] ?? post.status ?? "상태 미정"}
                  </span>
                  <h1 className="mt-3 text-[28px] font-extrabold tracking-[-0.03em] text-slate-950">{post.title}</h1>
                </div>
                {isMine && <PostActions postId={post.id} />}
              </div>

              <div className="mt-2 flex items-center gap-2 text-sm text-slate-400">
                <span>{post.authorEmail || "작성자 정보 없음"}</span>
                <span aria-hidden>·</span>
                <span>{formatDate(post.createdAt)}</span>
              </div>

              <p className="mt-6 whitespace-pre-line text-[15px] leading-relaxed text-slate-700">{post.content}</p>

              {post.images && post.images.length > 0 && (
                <div className="mt-6 grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {post.images.map((imgUrl, index) => (
                    <div key={index} className="relative aspect-video rounded-xl overflow-hidden bg-slate-100 border border-slate-200">
                      <img 
                        src={backendUrl(imgUrl)} 
                        alt={`수리 요청 이미지 ${index + 1}`} 
                        className="object-cover w-full h-full"
                      />
                    </div>
                  ))}
                </div>
              )}

              {!isMine && <RepairProposalForm postId={post.id} />}
            </div>

            {/* 제안 목록 섹션 */}
            <div className="dashboard-card">
              <h3 className="text-lg font-extrabold text-slate-900 mb-4">
                받은 수리 제안 <span className="text-blue-600">{proposals.length}</span>
              </h3>
              <ProposalList postId={post.id} proposals={proposals} isMine={isMine} userEmail={userEmail} />
            </div>
          </div>
        )}
      </main>
    </div>
  );
}