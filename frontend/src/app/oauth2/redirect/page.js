"use client";
import { useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useAuthStore } from "@/store/authStore";

function OAuth2RedirectContent() {
  const params = useSearchParams();
  const token = params.get("token");
  const refreshToken = params.get("refreshToken");
  const socialLogin = useAuthStore((state) => state.socialLogin);
  useEffect(() => {
    let active = true;
    if (!token) { window.location.replace("/login"); return; }
    socialLogin(token, refreshToken).then(() => {
      if (active) window.location.replace("/");
    }).catch(() => { if (active) window.location.replace("/login"); });
    return () => { active = false; };
  }, [token, refreshToken, socialLogin]);
  return <div className="flex min-h-screen items-center justify-center"><p>로그인 처리 중...</p></div>;
}
export default function OAuth2RedirectPage() {
  return <Suspense fallback={<p>로딩 중...</p>}><OAuth2RedirectContent /></Suspense>;
}
