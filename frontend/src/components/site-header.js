"use client";

import Link from "next/link";
import { Bell, ChatCircleDots, UserCircle } from "@phosphor-icons/react/dist/ssr";
import BrandLogo from "@/components/brand-logo";
import { useAuthStore } from "@/store/authStore";
import { useEffect, useState } from "react";

export default function SiteHeader({ userEmail: serverUserEmail }) {
  const { userEmail: storeEmail, initAuth } = useAuthStore();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    initAuth();
  }, [initAuth]);

  const cookieEmail = typeof document !== "undefined" 
    ? document.cookie.match(/user_email=([^;]+)/)?.[1] ? decodeURIComponent(document.cookie.match(/user_email=([^;]+)/)[1]) : null 
    : null;

  const userEmail = serverUserEmail || storeEmail || cookieEmail;

  return (
    <header className="site-header"><div className="page-shell header-inner">
      <BrandLogo />
      <nav className="desktop-nav" aria-label="주요 메뉴">
        <Link href="/" className="nav-link nav-link-active">홈</Link>
        <Link href="/posts/new" className="nav-link">수리 요청</Link> {/* <-- 이 부분을 수정 */}
        <Link href="/#start" className="nav-link">도움 주기</Link>
        {userEmail ? <Link href="/#posts" className="nav-link">채팅</Link> : <Link href="/#guide" className="nav-link">이용 가이드</Link>}
      </nav>
      {userEmail ? <div className="header-account"><span className="header-icon"><Bell size={20} /></span><span className="header-icon"><ChatCircleDots size={21} /></span><UserCircle size={29} weight="duotone" className="text-blue-600" /><span className="header-email">{userEmail}</span><form action="/api/auth/logout" method="post" onSubmit={() => {
        if (typeof window !== "undefined") {
          localStorage.removeItem("access_token");
          localStorage.removeItem("refresh_token");
        }
      }}><button type="submit">로그아웃</button></form></div>
        : <div className="header-actions"><Link href="/login" className="header-outline-button">로그인</Link><Link href="/signup" className="header-primary-button">회원가입</Link></div>}
    </div></header>
  );
}