// components/SiteHeader.jsx
"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { UserCircle } from "@phosphor-icons/react/dist/ssr";
import BrandLogo from "@/components/brand-logo";
import NotificationBell from "@/components/notification-bell";
import { useAuthStore } from "@/store/authStore";
import { useEffect, useState } from "react";

export default function SiteHeader({ userEmail: serverUserEmail }) {
  const pathname = usePathname();
  const router = useRouter();

  const {
    userEmail: storeEmail,
    initAuth,
    setLogout,
  } = useAuthStore();

  const [isAdmin, setIsAdmin] = useState(false);

  const [nickname, setNickname] = useState(() => {
    if (typeof window === "undefined") {
      return null;
    }

    try {
      return localStorage.getItem("nickname");
    } catch {
      return null;
    }
  });

  useEffect(() => {
    initAuth();
  }, [initAuth]);

  const cookieEmail =
    typeof document !== "undefined"
      ? document.cookie.match(/user_email=([^;]+)/)?.[1]
        ? decodeURIComponent(
            document.cookie.match(/user_email=([^;]+)/)[1]
          )
        : null
      : null;

  const userEmail =
    serverUserEmail ||
    storeEmail ||
    cookieEmail;

  const displayIsAdmin =
    Boolean(userEmail) && isAdmin;

  const displayNickname =
    userEmail ? nickname : null;

  useEffect(() => {
    if (!userEmail) {
      try {
        localStorage.removeItem("nickname");
      } catch {
        // 무시
      }

      return;
    }

    let active = true;

    fetch("/api/users/me", {
      cache: "no-store",
    })
      .then((res) =>
        res.ok ? res.json() : null
      )
      .then((me) => {
        if (!active) {
          return;
        }

        setIsAdmin(
          me?.role === "ADMIN"
        );

        const fetchedNickname =
          me?.nickname ?? null;

        setNickname(
          fetchedNickname
        );

        try {
          if (fetchedNickname) {
            localStorage.setItem(
              "nickname",
              fetchedNickname
            );
          } else {
            localStorage.removeItem(
              "nickname"
            );
          }
        } catch {
          // 무시
        }
      })
      .catch(() => {
        if (!active) {
          return;
        }

        setIsAdmin(false);
        setNickname(null);
      });

    return () => {
      active = false;
    };
  }, [userEmail]);

  const handleLogout = async (event) => {
    event.preventDefault();

    try {
      await fetch("/api/auth/logout", {
        method: "POST",
      });
    } catch (err) {
      console.error(
        "로그아웃 통신 실패:",
        err
      );
    } finally {
      setLogout();

      try {
        localStorage.removeItem(
          "nickname"
        );
      } catch {
        // 무시
      }

      router.push("/");
      router.refresh();
    }
  };

  return (
    <header className="site-header">
      <div className="page-shell header-inner">
        <BrandLogo />

        <nav
          className="desktop-nav"
          aria-label="주요 메뉴"
        >
          <Link
            href="/"
            className={`nav-link ${
              pathname === "/"
                ? "nav-link-active"
                : ""
            }`}
          >
            홈
          </Link>

          <Link
            href="/posts"
            className={`nav-link ${
              pathname.startsWith("/posts")
                ? "nav-link-active"
                : ""
            }`}
          >
            수리 요청
          </Link>

          {userEmail && (
            <Link
              href="/profile"
              className={`nav-link ${
                pathname.startsWith(
                  "/profile"
                )
                  ? "nav-link-active"
                  : ""
              }`}
            >
              내 프로필
            </Link>
          )}

          {displayIsAdmin && (
            <Link
              href="/admin"
              className={`nav-link ${
                pathname.startsWith(
                  "/admin"
                )
                  ? "nav-link-active"
                  : ""
              }`}
            >
              관리자
            </Link>
          )}
        </nav>

        {userEmail ? (
          <div className="header-account">
            <NotificationBell />

            <Link
              href="/profile"
              aria-label="내 프로필로 이동"
            >
              <UserCircle
                size={29}
                weight="duotone"
                className="text-blue-600"
              />
            </Link>

            <span className="header-email">
              {displayNickname ||
                userEmail}
            </span>

            <form
              onSubmit={handleLogout}
            >
              <button type="submit">
                로그아웃
              </button>
            </form>
          </div>
        ) : (
          <div className="header-actions">
            <Link
              href="/login"
              className="header-outline-button"
            >
              로그인
            </Link>

            <Link
              href="/signup"
              className="header-primary-button"
            >
              회원가입
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}
