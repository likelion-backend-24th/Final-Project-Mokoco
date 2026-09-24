// components/SiteHeader.jsx
"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { UserCircle } from "@phosphor-icons/react/dist/ssr";
import BrandLogo from "@/components/brand-logo";
import NotificationBell from "@/components/notification-bell";
import { useAuthStore } from "@/store/authStore";
import { useEffect, useRef, useState } from "react";

export default function SiteHeader({ userEmail: serverUserEmail }) {
  const pathname = usePathname();
  const { userEmail: storeEmail, accessToken, initAuth, setLogout } = useAuthStore();
  const [isAdmin, setIsAdmin] = useState(false);
  // 닉네임은 /api/users/me 응답이 오기 전까지 userEmail이 폴백으로 보여서 화면 전환마다
  // 이메일이 잠깐 스쳐 지나가는데, 직전에 받아둔 값을 첫 렌더부터 바로 쓰면(지연 초기화) 그
  // 틈이 없어진다 — effect에서 나중에 set하면 한 프레임 늦게 나타나 깜빡임이 남는다.
  const [nickname, setNickname] = useState(() => {
    if (typeof window === "undefined") return null;
    try { return localStorage.getItem("nickname"); } catch { return null; }
  });
  // AuthInitializer가 토큰 재발급 실패 시 배경에서 조용히 setLogout()을 부르는데, 페이지 이동이
  // 없으니 이 컴포넌트는 그대로 남아있다. 아래에서 serverUserEmail을 최우선으로 두는 이유는
  // 첫 렌더에서 로그인 여부가 깜빡이지 않게 하기 위해서인데, 그 우선순위 때문에 로그아웃이
  // 일어나도 헤더가 계속 로그인 상태로 보였다 — 세션 토큰이 있다가 사라지는 진짜 로그아웃
  // 전환만 감지해서 그때는 serverUserEmail보다 우선하도록 별도로 추적한다.
  const [loggedOut, setLoggedOut] = useState(false);
  const hadAccessTokenRef = useRef(false);

  useEffect(() => {
    if (accessToken) hadAccessTokenRef.current = true;
    else if (hadAccessTokenRef.current) setLoggedOut(true);
  }, [accessToken]);

  useEffect(() => {
    initAuth();
  }, [initAuth]);

  const cookieEmail = typeof document !== "undefined"
    ? document.cookie.match(/user_email=([^;]+)/)?.[1] ? decodeURIComponent(document.cookie.match(/user_email=([^;]+)/)[1]) : null
    : null;

  const userEmail = loggedOut ? null : (serverUserEmail || storeEmail || cookieEmail);

  // 관리자 메뉴 노출 여부 및 닉네임 표시 — 페이지마다 넘겨받을 필요 없이 헤더가 직접 확인한다.
  // userEmail이 없을 땐 아래 JSX가 이 값들을 아예 안 쓰니 굳이 리셋할 필요가 없고(캐시된 닉네임
  // 정리는 setLogout()이 이미 한다), 조회만 건너뛴다.
  useEffect(() => {
    if (!userEmail) return;
    let active = true;
    fetch("/api/users/me", { cache: "no-store" })
      .then((res) => (res.ok ? res.json() : null))
      .then((me) => {
        if (!active) return;
        setIsAdmin(me?.role === "ADMIN");
        const fetchedNickname = me?.nickname ?? null;
        setNickname(fetchedNickname);
        try {
          if (fetchedNickname) localStorage.setItem("nickname", fetchedNickname);
          else localStorage.removeItem("nickname");
        } catch { /* 무시 */ }
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
          <Link href="/profile" className={`nav-link ${pathname.startsWith("/profile") ? "nav-link-active" : ""}`}>내 프로필</Link>
        )}
        {isAdmin && userEmail && (
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