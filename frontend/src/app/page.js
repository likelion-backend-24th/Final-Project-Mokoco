import Link from "next/link";
import { cookies } from "next/headers";
import { ArrowRight, ClipboardText, HandHeart, Wrench } from "@phosphor-icons/react/dist/ssr";
import SiteHeader from "@/components/site-header";
import LocationPermissionPrompt from "@/components/location-permission-prompt";
import ActiveDealsCard from "@/components/active-deals-card";
import { imageSrc } from "@/lib/backend";
import { getNearbyPosts } from "@/lib/nearby-posts";
import { getMyActiveDeals } from "@/lib/active-deals";
import { htmlToText } from "@/lib/html-to-text";
import { formatRelativeDate } from "@/lib/format-relative-date";
import RegionScopeFilter from "@/components/region-scope-filter";
import { normalizeRegionScope, regionListHref } from "@/lib/region-scope";

export const dynamic = 'force-dynamic';
export const revalidate = 0;

const statusLabel = { WAITING: "도움 기다리는 중", MATCHED: "이웃과 연결됨", COMPLETED: "거래 완료" };

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
          {post.thumbnailUrl ? (
            <div className="post-icon overflow-hidden !p-0 border border-slate-200">
              <img src={imageSrc(post.thumbnailUrl)} alt="수리 요청 썸네일" loading="lazy" className="absolute inset-0 h-full w-full object-cover" />
            </div>
          ) : (
            <div className="post-icon" aria-hidden="true"><Wrench size={27} weight="duotone" /></div>
          )}
          <div className="min-w-0 flex-1">
            <div className="post-title-line"><h3>{post.title || "제목 없는 수리 요청"}</h3><time>{formatRelativeDate(post.createdAt)}</time></div>
            <div className="post-meta"><span className={`status-badge status-${post.status?.toLowerCase()}`}>{statusLabel[post.status] ?? post.status ?? "상태 미정"}</span><span>{post.authorNickname || post.authorEmail || "작성자 정보 없음"}</span></div>
            <p className="post-content">{htmlToText(post.content) || "등록된 상세 내용이 없습니다."}</p>
          </div>
        </Link>
      ))}
    </div>
  );
}

function UnifiedHome({ posts, error, userEmail, isAuthenticated, pagination, regionScope, activeDeals }) {
  return (
    <main className="page-shell auth-main">
      {isAuthenticated && <LocationPermissionPrompt userEmail={userEmail} />}
      {isAuthenticated && <RegionScopeFilter pathname="/" regionScope={regionScope} regionFilter={pagination?.regionFilter} />}
      <div className="auth-dashboard-grid">
        <div className="dashboard-column">
          <section id="posts" className="reference-card post-card">
            <div className="reference-card-heading">
              <h2>오늘의 수리 요청</h2>
              <Link href={regionListHref({ regionScope })}>전체 보기 <ArrowRight size={14} /></Link>
            </div>
            <PostList posts={posts} error={error} postHref={isAuthenticated ? "/posts/new" : "/login"} />
          </section>
          {!isAuthenticated && (
            <section className="reference-card">
              <div className="reference-card-heading"><h2>전체 수리 요청 현황</h2></div>
              <div className="neighborhood-summary">
                <Wrench size={38} weight="duotone" />
                <div><strong>{error ? "확인 불가" : `${pagination?.totalElements ?? 0}건`}</strong></div>
              </div>
            </section>
          )}
        </div>
        <aside className="dashboard-column">
          {isAuthenticated ? (
            <ActiveDealsCard deals={activeDeals} />
          ) : (
            <section className="reference-card activity-card text-center py-8">
              <HandHeart size={40} weight="duotone" className="mx-auto mb-2 text-blue-500" />
              <h2 className="mb-2">로그인하고 더 많은 기능을 이용해보세요</h2>
              <p className="text-sm text-slate-500 mb-4">내 수리 요청 현황을 관리하고 이웃과 소통할 수 있습니다.</p>
              <Link href="/login" className="compact-primary-button w-full justify-center">로그인하기</Link>
            </section>
          )}
        </aside>
      </div>
    </main>
  );
}

export default async function Home({ searchParams }) {
  const regionScope = normalizeRegionScope((await searchParams)?.regionScope);
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  const accessToken = cookieStore.get("access_token")?.value ?? null;
  
  const isAuthenticated = Boolean(userEmail && accessToken);

  const [{ posts, error, pagination }, activeDeals] = await Promise.all([
    getNearbyPosts(accessToken, "ALL", 0, 5, regionScope),
    isAuthenticated ? getMyActiveDeals(accessToken) : Promise.resolve({ requester: [], repairer: [] }),
  ]);
  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={isAuthenticated ? userEmail : null} />
      <UnifiedHome
        regionScope={regionScope} pagination={pagination} posts={posts} error={error}
        userEmail={userEmail} isAuthenticated={isAuthenticated} activeDeals={activeDeals}
      />
    </div>
  );
}
