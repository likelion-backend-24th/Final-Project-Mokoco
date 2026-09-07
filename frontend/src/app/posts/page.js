import Link from "next/link";
import { cookies } from "next/headers";
import { 
  ClipboardText, Plus, Wrench, SquaresFour, Lightbulb, 
  Drop, Hammer, WashingMachine, DoorOpen, Toolbox 
} from "@phosphor-icons/react/dist/ssr";
import SiteHeader from "@/components/site-header";
import { backendUrl } from "@/lib/backend";

const statusLabel = { WAITING: "도움 기다리는 중", MATCHED: "이웃과 연결됨", COMPLETED: "수리 완료" };

const categories = [
  { value: "ALL", label: "전체", icon: SquaresFour },
  { value: "ELECTRIC_LIGHT", label: "전기·조명", icon: Lightbulb },
  { value: "PLUMBING", label: "배관·설비", icon: Drop },
  { value: "FURNITURE_INSTALL", label: "가구·설치", icon: Hammer },
  { value: "HOME_APP_LIANCE", label: "가전제품", icon: WashingMachine },
  { value: "DOOR_WINDOW", label: "문·창문", icon: DoorOpen },
  { value: "LIVING_ETC", label: "생활·기타", icon: Toolbox },
];

async function getPosts(category) {
  try {
    const url = category && category !== "ALL" 
      ? backendUrl(`/posts?category=${category}`) 
      : backendUrl("/posts");
      
    const response = await fetch(url, { cache: "no-store", signal: AbortSignal.timeout(5000) });
    if (!response.ok) return { posts: [], error: "수리 요청을 불러오지 못했습니다." };
    const posts = await response.json();
    return Array.isArray(posts) ? { posts, error: null } : { posts: [], error: "백엔드 응답 형식이 올바르지 않습니다." };
  } catch {
    return { posts: [], error: "백엔드 서버에 연결할 수 없습니다. 서버 실행 상태를 확인해주세요." };
  }
}

function formatRelativeDate(value) {
  if (!value) return "시간 정보 없음";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "시간 정보 없음";
  const minutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  return hours < 24 ? `${hours}시간 전` : `${Math.floor(hours / 24)}일 전`;
}

function EmptyState({ error, postHref }) {
  return (
    <div className="reference-empty-state" role="status">
      {error ? <Wrench size={58} weight="duotone" /> : <ClipboardText size={58} weight="duotone" />}
      <h3>{error ? "데이터 연결을 확인해주세요" : "아직 등록된 수리 요청이 없어요"}</h3>
      <p>{error ?? "첫 번째 수리 요청을 올려보세요!"}</p>
      <Link href={postHref} className="compact-primary-button">수리 요청하기</Link>
    </div>
  );
}

export default async function PostsPage({ searchParams }) {
  const resolvedSearchParams = await searchParams;
  const currentCategory = resolvedSearchParams?.category || "ALL";

  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  const { posts, error } = await getPosts(currentCategory);
  const postHref = userEmail ? "/posts/new" : "/login";

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="page-shell auth-main">
        <div className="section-heading">
          <div>
            <p className="section-kicker">REPAIR POSTS</p>
            <h2>수리 요청</h2>
          </div>
          <Link href={postHref} className="compact-primary-button gap-1.5">
            <Plus size={16} weight="bold" />
            수리 요청 올리기
          </Link>
        </div>

        <div className="category-filter-row mb-6 overflow-x-auto pb-2" aria-label="수리 분야 필터">
          {categories.map(({ value, label, icon: Icon }) => {
            const isActive = currentCategory === value;
            return (
              <Link
                key={value}
                href={value === "ALL" ? "/posts" : `/posts?category=${value}`}
                className={`category-filter shrink-0 inline-flex items-center gap-2 ${isActive ? "category-filter-active" : ""}`}
              >
                <Icon size={20} weight="duotone" />
                <span>{label}</span>
              </Link>
            );
          })}
        </div>

        {error || posts.length === 0 ? (
          <EmptyState error={error} postHref={postHref} />
        ) : (
          <div className="post-list">
            {posts.map((post) => {
              const firstImage = post.images && post.images.length > 0 ? (typeof post.images[0] === "string" ? post.images[0] : post.images[0].imageUrl) : null;

              return (
                <Link key={post.id} href={`/posts/${post.id}`} className="post-row flex items-start gap-4">
                  {firstImage ? (
                    <div className="post-icon shrink-0 overflow-hidden !p-0 border border-slate-200 mt-1">
                      <img 
                        src={backendUrl(firstImage)} 
                        alt="수리 요청 썸네일" 
                        className="object-cover w-full h-full"
                      />
                    </div>
                  ) : (
                    <div className="post-icon shrink-0 mt-1" aria-hidden="true">
                      <Wrench size={27} weight="duotone" />
                    </div>
                  )}

                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2 mb-1 min-w-0">
                          <h3 className="truncate font-semibold text-slate-900">{post.title || "제목 없는 수리 요청"}</h3>
                          <span className={`shrink-0 status-badge status-${post.status?.toLowerCase()}`}>
                            {statusLabel[post.status] ?? post.status ?? "상태 미정"}
                          </span>
                        </div>
                      </div>
                      <div className="flex flex-col items-end shrink-0 text-xs text-slate-400 gap-0.5">
                        <time>{formatRelativeDate(post.createdAt)}</time>
                        <span>{post.authorEmail || "작성자 정보 없음"}</span>
                      </div>
                    </div>
                    <p className="post-content">{post.content || "등록된 상세 내용이 없습니다."}</p>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}