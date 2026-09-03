import Link from "next/link";
import { HandHeart, ShieldCheck, UsersThree } from "@phosphor-icons/react/dist/ssr";
import BrandLogo from "@/components/brand-logo";

export default function AuthShell({ variant, children }) {
  const isLogin = variant === "login";
  return (
    <div className="flex min-h-screen flex-col bg-[#f8faff]">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-20 max-w-[1450px] items-center justify-between px-6 sm:px-10">
          <BrandLogo />
          <p className="text-sm text-slate-500">{isLogin ? "처음이신가요?" : "이미 계정이 있나요?"}{" "}<Link className="font-bold text-blue-600 hover:underline" href={isLogin ? "/signup" : "/login"}>{isLogin ? "회원가입" : "로그인"}</Link></p>
        </div>
      </header>
      <main className="mx-auto grid w-full max-w-[1320px] flex-1 items-center gap-12 px-6 py-12 lg:grid-cols-[0.8fr_1.2fr] lg:px-10">
        <section className="hidden lg:block">
          <p className="text-sm font-bold tracking-[0.14em] text-blue-600">MOKOCO COMMUNITY</p>
          <h1 className="mt-5 whitespace-pre-line text-5xl font-bold leading-tight tracking-[-0.04em] text-slate-950">{isLogin ? "가까운 이웃과 연결되어\n수리가 쉬워지는 동네생활" : "우리 동네에서 서로 돕는\n수리 커뮤니티에 참여하세요"}</h1>
          <p className="mt-6 max-w-lg text-lg leading-8 text-slate-600">필요할 땐 요청하고, 가능할 땐 도와주며 서로 믿고 연결되는 따뜻한 동네를 만들어요.</p>
          <div className="mt-10 space-y-5">
            {[[UsersThree, "가까운 이웃과 연결"], [ShieldCheck, "믿을 수 있는 도움"], [HandHeart, "작은 수리로 큰 행복"]].map(([Icon, text]) => (
              <div key={text} className="flex items-center gap-4 font-semibold text-slate-800"><span className="grid size-12 place-items-center rounded-2xl bg-blue-100 text-blue-600"><Icon size={25} weight="duotone" /></span>{text}</div>
            ))}
          </div>
        </section>
        {children}
      </main>
    </div>
  );
}
