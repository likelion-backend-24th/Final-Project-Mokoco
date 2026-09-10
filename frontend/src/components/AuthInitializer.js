// components/AuthInitializer.js
"use client";

import { useEffect } from "react";
import { jwtDecode } from "jwt-decode";
import { useAuthStore } from "@/store/authStore";
import { useRouter } from "next/navigation";

// 액세스 토큰 만료 2분 전이면 미리 갱신
const REFRESH_MARGIN_MS = 120_000;

async function refreshIfNeeded({ force = false } = {}) {
  if (typeof window === "undefined") return;

  const token = localStorage.getItem("access_token");
  const refreshToken = localStorage.getItem("refresh_token");
  if (!token && !refreshToken) return; // 비로그인
  if (!refreshToken) return; // 갱신 수단 없음

  let needs = force || !token;
  if (!needs) {
    try {
      const { exp } = jwtDecode(token);
      needs = !exp || exp * 1000 - Date.now() < REFRESH_MARGIN_MS;
    } catch {
      needs = true;
    }
  }
  if (!needs) return;

  let email;
  try {
    email = jwtDecode(token || refreshToken)?.sub;
  } catch {
    /* 무시 */
  }

  try {
    const res = await fetch("/api/auth/reissue", {
      method: "POST",
      cache: "no-store",
      headers: email ? { "Content-Type": "application/json" } : undefined,
      body: email ? JSON.stringify({ email }) : undefined,
    });
    if (res.status === 400 || res.status === 401) {
      // refresh_token 만료/무효 -> 세션 종료
      useAuthStore.getState().setLogout();
      return;
    }
    if (!res.ok) return; // 5xx/네트워크: 다음 주기에 재시도
    const data = await res.json();
    if (data.accessToken) {
      localStorage.setItem("access_token", data.accessToken);
      if (data.refreshToken) localStorage.setItem("refresh_token", data.refreshToken);
      try {
        const email = jwtDecode(data.accessToken).sub;
        useAuthStore.setState({ accessToken: data.accessToken, ...(email ? { userEmail: email } : {}) });
      } catch {
        useAuthStore.setState({ accessToken: data.accessToken });
      }
    }
  } catch {
    /* 네트워크 오류 시 다음 주기에 재시도 */
  }
}

export default function AuthInitializer() {
  const initAuth = useAuthStore((state) => state.initAuth);
  const router = useRouter();

  useEffect(() => {
    initAuth();
    refreshIfNeeded();

    // 주기적 갱신 (30분 만료 토큰이라 1분 간격이면 충분)
    const interval = setInterval(() => refreshIfNeeded(), 60_000);

    // 탭 복귀 시 즉시 확인
    const onVisible = () => {
      if (document.visibilityState === "visible") refreshIfNeeded();
    };

    // bfcache 복원 시 상태 재동기화
    const handlePageShow = (event) => {
      if (event.persisted) {
        initAuth();
        refreshIfNeeded();
        router.refresh();
      }
    };

    document.addEventListener("visibilitychange", onVisible);
    window.addEventListener("pageshow", handlePageShow);
    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", onVisible);
      window.removeEventListener("pageshow", handlePageShow);
    };
  }, [initAuth, router]);

  return null;
}
