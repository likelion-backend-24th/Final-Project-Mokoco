import Link from "next/link";
import BrandLogo from "@/components/brand-logo";

export default function SiteHeader({ userEmail }) {
  return (
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex h-[72px] max-w-[1450px] items-center justify-between px-5 sm:px-8">
        <BrandLogo />
        <nav className="hidden h-full items-center gap-10 text-[15px] font-semibold text-slate-600 md:flex" aria-label="주요 메뉴">
          <Link href="/" className="nav-link nav-link-active">홈</Link>
          <Link href="/#requests" className="nav-link">수리 요청</Link>
          <Link href="/#start" className="nav-link">도움 주기</Link>
          <Link href="/#guide" className="nav-link">이용 가이드</Link>
        </nav>
        {userEmail ? (
          <div className="flex items-center gap-3">
            <span className="hidden max-w-48 truncate text-sm font-semibold text-slate-700 sm:block">{userEmail}</span>
            <form action="/api/auth/logout" method="post"><button className="header-outline-button" type="submit">로그아웃</button></form>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Link href="/login" className="header-outline-button">로그인</Link>
            <Link href="/signup" className="header-primary-button">회원가입</Link>
          </div>
        )}
      </div>
    </header>
  );
}
