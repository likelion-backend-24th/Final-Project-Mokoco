import Link from "next/link";
import { cookies } from "next/headers";
import {
  ArrowRight, ClipboardText, DoorOpen, Drop, Hammer, HandHeart,
  Lightbulb, MapTrifold, SquaresFour, Star, Toolbox, UserCircle,
  WashingMachine, Wrench,
} from "@phosphor-icons/react/dist/ssr";
import SiteHeader from "@/components/site-header";
import LocationPermissionPrompt from "@/components/location-permission-prompt";
import { backendUrl } from "@/lib/backend";

const statusLabel = { WAITING: "도움 기다리는 중", MATCHED: "이웃과 연결됨", COMPLETED: "수리 완료" };
const categories = [
  [SquaresFour, "전체"], [Lightbulb, "전기·조명"], [Drop, "배관·설비"],
  [Hammer, "가구·설치"], [WashingMachine, "가전제품"], [DoorOpen, "문·창문"], [Toolbox, "생활·기타"],
];

async function getPosts() {
  try {
    const response = await fetch(backendUrl("/posts"), { cache: "no-store", signal: AbortSignal.timeout(5000) });
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

function EmptyRequests({ error, requestHref }) {
  return (
    <div className="reference-empty-state" role="status">
      {error ? <Wrench size={58} weight="duotone" /> : <ClipboardText size={58} weight="duotone" />}
      <h3>{error ? "데이터 연결을 확인해주세요" : "아직 등록된 수리 요청이 없어요"}</h3>
      <p>{error ?? "첫 번째 수리 요청을 올려보세요!"}</p>
      <Link href={requestHref} className="compact-primary-button">수리 요청하기</Link>
    </div>
  );
}

function RequestList({ posts, error, requestHref }) {
  if (error || posts.length === 0) return <EmptyRequests error={error} requestHref={requestHref} />;
  return (
    <div className="request-list">
      {posts.slice(0, 5).map((post) => (
        <Link key={post.id} href={`/requests/${post.id}`} className="request-row">
          <div className="request-icon" aria-hidden="true"><Wrench size={27} weight="duotone" /></div>
          <div className="min-w-0 flex-1">
            <div className="request-title-line"><h3>{post.title || "제목 없는 수리 요청"}</h3><time>{formatRelativeDate(post.createdAt)}</time></div>
            <div className="request-meta"><span className={`status-badge status-${post.status?.toLowerCase()}`}>{statusLabel[post.status] ?? post.status ?? "상태 미정"}</span><span>{post.authorEmail || "작성자 정보 없음"}</span></div>
            <p className="request-content">{post.content || "등록된 상세 내용이 없습니다."}</p>
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

function UnauthenticatedHome({ posts, error }) {
  return (
    <><main>
      <section className="unauth-hero"><div className="page-shell hero-grid">
        <div><h1>가까운 이웃과 연결되어<br />수리가 쉬워지는 <span>동네생활</span></h1>
          <p>생활 속 작은 수리부터 전문가의 도움이 필요한 일까지,<br className="hidden sm:block" /> 우리 동네에서 쉽고 빠르게 해결해보세요.</p>
          <div className="hero-actions"><Link href="/login" className="compact-primary-button">수리 요청하기</Link><Link href="/signup" className="compact-outline-button">도움 주기</Link></div>
        </div>
        <div className="hero-visual" aria-hidden="true"><MapTrifold size={190} weight="duotone" /></div>
      </div></section>
      <section className="page-shell unauth-section"><h2>어떤 수리를 찾고 계신가요?</h2><CategoryRow /></section>
      <section id="requests" className="page-shell unauth-section recent-section"><h2>최근 올라온 수리 요청</h2><RequestList posts={posts} error={error} requestHref="/login" /></section>
      <section id="guide" className="page-shell community-banner"><HandHeart size={50} weight="duotone" /><div><h2>이웃과 함께 만드는 따뜻한 동네 커뮤니티</h2><p>도움이 필요한 이웃을 돕고, 나도 도움을 받을 수 있어요.</p></div><Link href="/signup" className="compact-outline-button">이용 가이드 보기</Link></section>
    </main><Footer /></>
  );
}

function AuthenticatedHome({ posts, error, userEmail }) {
  const myRequests = posts.filter((post) => post.authorEmail === userEmail);
  const inProgress = myRequests.filter((post) => post.status === "MATCHED").length;
  const completed = myRequests.filter((post) => post.status === "COMPLETED").length;
  return (
    <><main className="page-shell auth-main">
      <LocationPermissionPrompt userEmail={userEmail} /><CategoryRow compact />
      <div className="auth-dashboard-grid">
        <div className="dashboard-column">
          <section id="requests" className="reference-card request-card"><div className="reference-card-heading"><h2>오늘의 수리 요청</h2><Link href="/requests">전체 보기 <ArrowRight size={14} /></Link></div><RequestList posts={posts} error={error} requestHref="/requests/new" /></section>
          <section className="reference-card"><div className="reference-card-heading"><h2>우리 동네 요청 현황</h2></div><div className="neighborhood-summary"><Wrench size={38} weight="duotone" /><div><strong>{error ? "확인 불가" : `${posts.length}건`}</strong><span>백엔드에서 조회된 전체 수리 요청</span></div></div></section>
        </div>
        <aside className="dashboard-column">
          <section className="reference-card activity-card"><h2>내 활동 요약</h2><dl>
            <div><dt><ClipboardText size={20} weight="duotone" />내가 올린 요청</dt><dd>{myRequests.length}건</dd></div>
            <div><dt><Wrench size={20} weight="duotone" />진행 중 요청</dt><dd>{inProgress}건</dd></div>
            <div><dt><Star size={20} weight="duotone" />완료한 요청</dt><dd>{completed}건</dd></div>
          </dl><Link href="#requests" className="wide-outline-button">내 활동 보기</Link></section>
          <section id="start" className="help-card"><span><Toolbox size={50} weight="duotone" /></span><div><h2>내가 가진 재능으로<br />이웃을 도와주세요</h2><p>작은 도움이 큰 힘이 됩니다.</p></div><Link href="#requests" className="wide-outline-button">도움 주기 시작하기</Link></section>
          <section className="reference-card signed-in-card"><UserCircle size={28} weight="duotone" /><div><span>로그인 계정</span><strong>{userEmail}</strong></div></section>
        </aside>
      </div>
    </main><Footer /></>
  );
}

export default async function Home() {
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("mokoco_user_email")?.value ?? null;
  const { posts, error } = await getPosts();
  return <div className="min-h-screen bg-[#f7f9fc]"><SiteHeader userEmail={userEmail} />{userEmail ? <AuthenticatedHome posts={posts} error={error} userEmail={userEmail} /> : <UnauthenticatedHome posts={posts} error={error} />}</div>;
}