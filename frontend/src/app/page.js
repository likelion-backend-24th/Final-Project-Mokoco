import Link from "next/link";
import { cookies } from "next/headers";
import {
  ArrowRight, ChatCircleDots, CheckCircle, HandHeart, Lightning,
  MapPin, ShieldCheck, UsersThree, Wrench,
} from "@phosphor-icons/react/dist/ssr";
import SiteHeader from "@/components/site-header";
import LocationPermissionPrompt from "@/components/location-permission-prompt";
import { backendUrl } from "@/lib/backend";

const statusLabel = {
  WAITING: "도움 기다리는 중",
  MATCHED: "이웃과 연결됨",
  COMPLETED: "수리 완료",
};

async function getPosts() {
  try {
    const response = await fetch(backendUrl("/posts"), {
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) return { posts: [], error: "수리 요청을 불러오지 못했습니다." };
    const posts = await response.json();
    return Array.isArray(posts)
      ? { posts, error: null }
      : { posts: [], error: "백엔드 응답 형식이 올바르지 않습니다." };
  } catch {
    return { posts: [], error: "백엔드 서버에 연결할 수 없습니다. 서버 실행 상태를 확인해주세요." };
  }
}

function formatDate(value) {
  if (!value) return "날짜 정보 없음";
  return new Intl.DateTimeFormat("ko-KR", { month: "long", day: "numeric" }).format(new Date(value));
}

export default async function Home() {
  const cookieStore = await cookies();
  const userEmail = cookieStore.get("mokoco_user_email")?.value ?? null;
  const { posts, error } = await getPosts();

  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      {userEmail && <LocationPermissionPrompt userEmail={userEmail} />}
      <main className="mx-auto w-full max-w-[1450px] px-5 py-6 sm:px-8">
        <section className="overflow-hidden rounded-[22px] border border-slate-200 bg-white shadow-[0_8px_30px_rgba(15,23,42,0.05)]">
          <div className="grid lg:grid-cols-[1.15fr_0.85fr]">
            <div className="px-7 py-10 sm:px-12 sm:py-14">
              <span className="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-600">
                <HandHeart size={19} weight="fill" /> 우리 동네의 작은 수리, 이웃이 함께 해결해요
              </span>
              <h1 className="mt-6 max-w-3xl text-4xl font-bold leading-[1.2] tracking-[-0.04em] text-slate-950 sm:text-5xl lg:text-[58px]">
                가까운 이웃과 연결되어<br />수리가 쉬워지는 <span className="text-blue-600">동네생활</span>
              </h1>
              <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">
                도움이 필요할 땐 요청하고, 내가 할 수 있는 일은 나누세요. 실제 등록된 수리 요청을 확인하고 믿을 수 있는 이웃과 연결됩니다.
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                {userEmail ? (
                  <Link className="primary-button" href="/#requests">수리 요청 보기 <ArrowRight size={19} weight="bold" /></Link>
                ) : (
                  <>
                    <Link className="primary-button" href="/signup">무료로 시작하기 <ArrowRight size={19} weight="bold" /></Link>
                    <Link className="secondary-button" href="/login">로그인</Link>
                  </>
                )}
              </div>
            </div>
            <div className="border-t border-slate-200 bg-blue-600 p-7 text-white lg:border-l lg:border-t-0 sm:p-10">
              <p className="text-sm font-semibold text-blue-100">MOKOCO COMMUNITY</p>
              <h2 className="mt-3 text-3xl font-bold leading-tight">{userEmail ? "내 동네 설정을 기준으로 연결할게요" : "로그인하면 내 동네를 중심으로 연결돼요"}</h2>
              <div className="mt-8 space-y-4">
                {[
                  [MapPin, "내 지역의 가까운 수리 요청 확인"],
                  [ChatCircleDots, "도움을 주고받을 이웃과 대화"],
                  [ShieldCheck, "요청 상태와 약속을 안전하게 관리"],
                ].map(([Icon, text]) => (
                  <div key={text} className="flex items-center gap-4 rounded-2xl bg-white/10 px-5 py-4">
                    <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-white text-blue-600"><Icon size={23} weight="duotone" /></span>
                    <span className="font-semibold">{text}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        <section id="requests" className="mt-6 grid gap-6 lg:grid-cols-[1.45fr_0.75fr]">
          <div className="dashboard-card">
            <div className="section-heading">
              <div><p className="section-kicker">LIVE REQUESTS</p><h2>지금 올라온 수리 요청</h2></div>
              <span className="rounded-full bg-blue-50 px-3 py-1.5 text-sm font-semibold text-blue-600">백엔드 실시간 조회</span>
            </div>
            {error ? (
              <div className="empty-state" role="status">
                <Lightning size={34} className="text-amber-500" />
                <div><p className="font-semibold text-slate-900">데이터 연결을 확인해주세요</p><p className="mt-1 text-sm text-slate-500">{error}</p></div>
              </div>
            ) : posts.length === 0 ? (
              <div className="empty-state" role="status">
                <Wrench size={34} className="text-blue-600" />
                <div><p className="font-semibold text-slate-900">아직 등록된 수리 요청이 없습니다</p><p className="mt-1 text-sm text-slate-500">첫 번째 요청은 로그인 후 등록할 수 있어요.</p></div>
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {posts.slice(0, 6).map((post) => (
                  <article key={post.id} className="py-5 first:pt-1 last:pb-1">
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className={`status-badge status-${post.status?.toLowerCase()}`}>{statusLabel[post.status] ?? post.status ?? "상태 미정"}</span>
                          <span className="text-sm text-slate-400">{formatDate(post.createdAt)}</span>
                        </div>
                        <h3 className="mt-3 truncate text-lg font-bold text-slate-900">{post.title}</h3>
                        <p className="mt-1 line-clamp-2 text-sm leading-6 text-slate-500">{post.content}</p>
                      </div>
                      <div className="flex shrink-0 items-center gap-2 text-sm text-slate-500"><UsersThree size={19} />{post.authorEmail}</div>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>

          <aside id="start" className="dashboard-card flex flex-col">
            <p className="section-kicker">GET STARTED</p>
            <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-950">처음 오셨나요?</h2>
            <p className="mt-3 leading-7 text-slate-600">계정을 만들고 지역을 설정하면 가까운 이웃의 요청을 더 빠르게 확인할 수 있습니다.</p>
            <div className="mt-7 space-y-3">
              {["이메일로 간단하게 가입", "실제 수리 요청 데이터 확인", "로그인 후 도움 요청과 참여"].map((item) => (
                <div key={item} className="flex items-center gap-3 text-sm font-medium text-slate-700"><CheckCircle size={20} weight="fill" className="text-emerald-500" />{item}</div>
              ))}
            </div>
            <Link href="/signup" className="primary-button mt-8 w-full justify-center">회원가입</Link>
            <p className="mt-4 text-center text-sm text-slate-500">이미 계정이 있나요? <Link href="/login" className="font-semibold text-blue-600 hover:underline">로그인</Link></p>
          </aside>
        </section>

        <section id="guide" className="mt-6 grid gap-4 sm:grid-cols-3">
          {[
            [Wrench, "수리 요청", "고장 난 물건과 필요한 도움을 구체적으로 남겨요."],
            [HandHeart, "이웃의 도움", "내가 잘하는 일로 가까운 이웃에게 손을 내밀어요."],
            [ShieldCheck, "안심 연결", "요청 상태를 확인하며 안전하게 약속을 관리해요."],
          ].map(([Icon, title, description]) => (
            <div key={title} className="dashboard-card flex items-start gap-4">
              <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-blue-50 text-blue-600"><Icon size={25} weight="duotone" /></span>
              <div><h2 className="font-bold text-slate-900">{title}</h2><p className="mt-1 text-sm leading-6 text-slate-500">{description}</p></div>
            </div>
          ))}
        </section>
      </main>

      <footer className="mt-8 border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-[1450px] flex-col gap-3 px-8 py-7 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p>© 2026 Mokoco. 가까운 이웃과 함께하는 수리 커뮤니티.</p>
          <div className="flex gap-5"><span>이용약관</span><span>개인정보처리방침</span><span>고객센터</span></div>
        </div>
      </footer>
    </div>
  );
}
