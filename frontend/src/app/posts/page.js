import Link from "next/link";
import { cookies } from "next/headers";
import {
  ClipboardText, Wrench, SquaresFour, Lightbulb,
  Drop, Hammer, WashingMachine, DoorOpen, Toolbox, MapPin
} from "@phosphor-icons/react/dist/ssr";
import SiteHeader from "@/components/site-header";
import LocationPermissionPrompt from "@/components/location-permission-prompt";
import { imageSrc } from "@/lib/backend";
import { getNearbyPosts } from "@/lib/nearby-posts";
import RegionScopeFilter from "@/components/region-scope-filter";
import { normalizeRegionScope, regionListHref } from "@/lib/region-scope";

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
      <h3>{error ? "수리 요청을 확인해주세요" : "아직 등록된 수리 요청이 없어요"}</h3>
      <p>{error ?? "첫 번째 수리 요청을 올려보세요!"}</p>
      <Link href={postHref} className="compact-primary-button">수리 요청하기</Link>
    </div>
  );
}

export default async function PostsPage({ searchParams }) {
  const resolvedSearchParams = await searchParams;
  const currentCategory = resolvedSearchParams?.category || "ALL";
  const regionScope = normalizeRegionScope(resolvedSearchParams?.regionScope);

  const cookieStore = await cookies();
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  const accessToken = cookieStore.get("access_token")?.value ?? null;
  const page = resolvedSearchParams?.page ?? "0";
  const { posts, error, pagination } = await getNearbyPosts(accessToken, currentCategory, page, 20, regionScope);
  const postHref = userEmail ? "/posts/new" : "/login";

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <main className="page-shell auth-main">
        {accessToken && <LocationPermissionPrompt userEmail={userEmail} />}
        {accessToken
          ? <RegionScopeFilter regionScope={regionScope} regionFilter={pagination?.regionFilter} category={currentCategory} />
          : <p className="mb-5 text-sm text-slate-500">전체 지역의 수리 요청입니다. 로그인하면 내 활동 지역으로 좁혀볼 수 있어요.</p>}

        <div className="section-heading">
          <div>
            <p className="section-kicker">REPAIR POSTS</p>
            <h2>수리 요청</h2>
          </div>
        </div>

        <div className="category-filter-row mb-6 overflow-x-auto pb-2" aria-label="수리 분야 필터">
          {categories.map(({ value, label, icon: Icon }) => {
            const isActive = currentCategory === value;
            return (
              <Link
                key={value}
                href={regionListHref({ category: value, regionScope })}
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
              const firstImage = post.thumbnailUrl;

              return (
                <Link key={post.id} href={`/posts/${post.id}`} className="post-row flex items-start gap-4">
                  {firstImage ? (
                    <div className="post-icon shrink-0 overflow-hidden !p-0 border border-slate-200 mt-1">
                      <img
                        src={imageSrc(firstImage)}
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
                        <div className="flex items-center gap-2 mb-1 min-w-0 flex-wrap">
                          <h3 className="truncate font-semibold text-slate-900">{post.title || "제목 없는 수리 요청"}</h3>
                          <span className={`shrink-0 status-badge status-${post.status?.toLowerCase()}`}>
                            {statusLabel[post.status] ?? post.status ?? "상태 미정"}
                          </span>

                          {/* 지역 이름 뱃지 우선 노출 (없으면 기존 코드 대체) */}
                          {(post.regionName || post.regionCode) && (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium bg-slate-100 text-slate-600 rounded-full shrink-0">
                              <MapPin size={12} weight="duotone" />
                              {post.regionName || post.regionCode}
                            </span>
                          )}
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

        {pagination && (
          <nav className="flex justify-center items-center gap-4 mt-6" aria-label="수리 요청 페이지">
            {!pagination.first && <Link href={regionListHref({ category: currentCategory, regionScope, page: pagination.number - 1 })}>이전</Link>}
            <span>{pagination.number + 1}페이지 · 총 {pagination.totalElements}건</span>
            {!pagination.last && <Link href={regionListHref({ category: currentCategory, regionScope, page: pagination.number + 1 })}>다음</Link>}
          </nav>
        )}
      </main>
    </div>
  );
}
