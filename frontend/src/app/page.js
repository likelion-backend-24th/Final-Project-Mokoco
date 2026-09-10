import Link from "next/link";
import { cookies } from "next/headers";
import {
  ArrowRight, ClipboardText, DoorOpen, Drop, Hammer,
  Lightbulb, SquaresFour, Toolbox, UserCircle,
  WashingMachine, Wrench,
} from "@phosphor-icons/react/dist/ssr";
import SiteHeader from "@/components/site-header";
import LocationPermissionPrompt from "@/components/location-permission-prompt";
import HomeChatList from "@/components/home-chat-list";
import { getNearbyPosts } from "@/lib/nearby-posts";
import RegionScopeFilter from "@/components/region-scope-filter";
import { normalizeRegionScope, regionListHref } from "@/lib/region-scope";

export const dynamic = 'force-dynamic';
export const revalidate = 0;

const statusLabel = { WAITING: "도움 기다리는 중", MATCHED: "이웃과 연결됨", COMPLETED: "수리 완료" };
const categories = [
  [SquaresFour, "전체"], [Lightbulb, "전기·조명"], [Drop, "배관·설비"],
  [Hammer, "가구·설치"], [WashingMachine, "가전제품"], [DoorOpen, "문·창문"], [Toolbox, "생활·기타"],
];


function formatRelativeDate(value) {
  if (!value) return "시간 정보 없음";

  let date;
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0, s = 0] = value;
    date = new Date(y, m - 1, d, h, min, s);
  } else {
    date = new Date(value);
  }

  if (Number.isNaN(date.getTime())) return "시간 정보 없음";
  
  const minutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  return hours < 24 ? `${hours}시간 전` : `${Math.floor(hours / 24)}일 전`;
}

function CategoryRow({ compact = false }) {
  return (
    <div className={compact ? "category-filter-row" : "category-showcase"} aria-label="수리 분야">
      {categories.map(([Icon, label], index) => (
        <div key={label} className={`${compact ? "category-filter" : "category-tile"} ${index === 0 && compact ? "category-filter-active" : ""}`}>
          <Icon size={compact ? 20 : 30} weight="duotone" /><span>{label}</span>
        </div>
      ))}
    </div>
  );
}

function EmptyPosts({ error, postHref }) {
  return (
    <div className="reference-empty-state" role="status">
      {error ? <Wrench size={58} weight="duotone" /> : <ClipboardText size={58} weight="duotone" />}
      <h3>{error ? "수리 요청을 확인해주세요" : "아직 등록된 수리 요청이 없어요"}</h3>
      <p>{error ?? "첫 번째 수리 요청을 올려보세요!"}</p>
      <Link href={postHref} className="compact-primary-button">수리 요청하기</Link>
    </div>
  );
}

function PostList({ posts, error, postHref }) {
  if (error || posts.length === 0) return <EmptyPosts error={error} postHref={postHref} />;
  return (
    <div className="post-list">
      {posts.slice(0, 5).map((post) => (
        <Link key={post.id} href={`/posts/${post.id}`} className="post-row">
          <div className="post-icon" aria-hidden="true"><Wrench size={27} weight="duotone" /></div>
          <div className="min-w-0 flex-1">
            <div className="post-title-line"><h3>{post.title || "제목 없는 수리 요청"}</h3><time>{formatRelativeDate(post.createdAt)}</time></div>
            <div className="post-meta"><span className={`status-badge status-${post.status?.toLowerCase()}`}>{statusLabel[post.status] ?? post.status ?? "상태 미정"}</span><span>{post.authorEmail || "작성자 정보 없음"}</span></div>
            <p className="post-content">{post.content || "등록된 상세 내용이 없습니다."}</p>
          </div>
        </Link>
      ))}
    </div>
  );
}

function Footer() {
  return (
    <footer className="site-footer"><div className="page-shell footer-inner">
      <div><strong>동네수리</strong><p>© 2026 동네수리. All rights reserved.</p></div>
      <div className="footer-links"><span>이용약관</span><span>개인정보처리방침</span><span>고객센터</span></div>
    </div></footer>
  );
}

function UnifiedHome({ posts, error, userEmail, isAuthenticated, pagination, regionScope }) {
  return (
    <><main className="page-shell auth-main">
      {isAuthenticated && <LocationPermissionPrompt userEmail={userEmail} />}
      {isAuthenticated && <RegionScopeFilter pathname="/" regionScope={regionScope} regionFilter={pagination?.regionFilter} />}
      <CategoryRow compact />
      <div className="auth-dashboard-grid">
        <div className="dashboard-column">
          <section id="posts" className="reference-card post-card">
            <div className="reference-card-heading">
              <h2>오늘의 수리 요청</h2>
              <Link href={regionListHref({ regionScope })}>전체 보기 <ArrowRight size={14} /></Link>
            </div>
            <PostList posts={posts} error={error} postHref={isAuthenticated ? "/posts/new" : "/login"} />
          </section>
          <section className="reference-card">
            <div className="reference-card-heading"><h2>{isAuthenticated ? "선택한 지역의 요청 현황" : "전체 수리 요청 현황"}</h2></div>
            <div className="neighborhood-summary">
              <Wrench size={38} weight="duotone" />
              <div><strong>{error ? "확인 불가" : `${pagination?.totalElements ?? 0}건`}</strong><span>{isAuthenticated ? "활동 지역에서 제안을 기다리는 공개 요청" : ""}</span></div>
            </div>
          </section>
        </div>
        <aside className="dashboard-column">
          {isAuthenticated ? (
            <section className="reference-card activity-card">
              <h2>내 활동 요약</h2>
              <p>활동 지역의 공개 수리 요청을 확인하고 이웃에게 제안해보세요.</p>
              <Link href={regionListHref({ regionScope })} className="wide-outline-button">주변 요청 보기</Link>
            </section>
          ) : (
            <section className="reference-card activity-card text-center py-8">
              <h2 className="mb-2">로그인하고 더 많은 기능을 이용해보세요</h2>
              <p className="text-sm text-slate-500 mb-4">내 수리 요청 현황을 관리하고 이웃과 소통할 수 있습니다.</p>
              <Link href="/login" className="compact-primary-button w-full justify-center">로그인하기</Link>
            </section>
          )}

          <HomeChatList key={userEmail || "guest"} isAuthenticated={isAuthenticated} />

          {isAuthenticated && (
            <section className="reference-card signed-in-card">
              <UserCircle size={28} weight="duotone" />
              <div><span>로그인 계정</span><strong>{userEmail}</strong></div>
            </section>
          )}
        </aside>
      </div>
    </main><Footer /></>
  );
}

export default async function Home({ searchParams }) {
  const regionScope = normalizeRegionScope((await searchParams)?.regionScope);
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  const accessToken = cookieStore.get("access_token")?.value ?? null;
  
  const isAuthenticated = Boolean(userEmail && accessToken);

  const { posts, error, pagination } = await getNearbyPosts(accessToken, "ALL", 0, 5, regionScope);
  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={isAuthenticated ? userEmail : null} />
      <UnifiedHome regionScope={regionScope} pagination={pagination} posts={posts} error={error} userEmail={userEmail} isAuthenticated={isAuthenticated} />
    </div>
  );
}
