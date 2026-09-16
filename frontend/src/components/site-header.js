// components/SiteHeader.jsx
"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { UserCircle } from "@phosphor-icons/react/dist/ssr";
import BrandLogo from "@/components/brand-logo";
import NotificationBell from "@/components/notification-bell";
import { useAuthStore } from "@/store/authStore";
import { useEffect, useState } from "react";

export default function SiteHeader({ userEmail: serverUserEmail }) {
  const pathname = usePathname();
  const { userEmail: storeEmail, initAuth, setLogout } = useAuthStore();
  const [mounted, setMounted] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const [nickname, setNickname] = useState(null);

  useEffect(() => {
    setMounted(true);
    initAuth();
  }, [initAuth]);

  const cookieEmail = typeof document !== "undefined"
    ? document.cookie.match(/user_email=([^;]+)/)?.[1] ? decodeURIComponent(document.cookie.match(/user_email=([^;]+)/)[1]) : null
    : null;

  const userEmail = serverUserEmail || storeEmail || cookieEmail;

  // 관리자 메뉴 노출 여부 및 닉네임 표시 — 페이지마다 넘겨받을 필요 없이 헤더가 직접 확인한다.
  useEffect(() => {
    if (!userEmail) { setIsAdmin(false); setNickname(null); return; }
    let active = true;
    fetch("/api/users/me", { cache: "no-store" })
      .then((res) => (res.ok ? res.json() : null))
      .then((me) => {
        if (!active) return;
        setIsAdmin(me?.role === "ADMIN");
        setNickname(me?.nickname ?? null);
      })
      .catch(() => { if (active) { setIsAdmin(false); setNickname(null); } });
    return () => { active = false; };
  }, [userEmail]);

  const handleLogout = async (e) => {
    e.preventDefault();
    try {
      // 1. 서버사이드 쿠키를 박살내는 로그아웃 API 호출
      await fetch("/api/auth/logout", {
        method: "POST",
      });
    } catch (err) {
      console.error("로그아웃 통신 실패:", err);
    } finally {
      // 2. 클라이언트 스토어 비우기 및 홈으로 리다이렉트
      setLogout();
      window.location.href = "/";
    }
  };

  return (
    <header className="site-header"><div className="page-shell header-inner">
      <BrandLogo />
      <nav className="desktop-nav" aria-label="주요 메뉴">
        <Link href="/" className={`nav-link ${pathname === "/" ? "nav-link-active" : ""}`}>홈</Link>
        <Link href="/posts" className={`nav-link ${pathname.startsWith("/posts") ? "nav-link-active" : ""}`}>수리 요청</Link>
        {userEmail && (
          <Link href="/settlements" className={`nav-link ${pathname.startsWith("/settlements") ? "nav-link-active" : ""}`}>정산 내역</Link>
        )}
        {userEmail && (
          <Link href="/profile" className={`nav-link ${pathname.startsWith("/profile") ? "nav-link-active" : ""}`}>내 프로필</Link>
        )}
        {isAdmin && (
          <Link href="/admin" className={`nav-link ${pathname.startsWith("/admin") ? "nav-link-active" : ""}`}>관리자</Link>
        )}
      </nav>
      {userEmail ? (
        <div className="header-account">
          <NotificationBell />
          <Link href="/profile" aria-label="내 프로필로 이동">
            <UserCircle size={29} weight="duotone" className="text-blue-600" />
          </Link>
          <span className="header-email">{nickname || userEmail}</span>
          <form onSubmit={handleLogout}>
            <button type="submit">로그아웃</button>
          </form>
        </div>
      ) : (
        <div className="header-actions">
          <Link href="/login" className="header-outline-button">로그인</Link>
          <Link href="/signup" className="header-primary-button">회원가입</Link>
        </div>
      )}
    </div></header>
  );
}