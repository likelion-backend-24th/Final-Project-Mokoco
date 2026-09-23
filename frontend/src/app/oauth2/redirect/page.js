// app/oauth2/redirect/page.jsx 수정
"use client";

import { useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/store/authStore";

const ERROR_MESSAGES = {
  social_account_linked_elsewhere: "이미 다른 계정에 연결된 소셜 계정이에요. 그 계정으로 로그인하려면 먼저 로그아웃해주세요.",
  link_user_not_found: "연결할 계정을 찾지 못했어요. 다시 로그인한 뒤 시도해주세요.",
  oauth_failed: "소셜 로그인 처리 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.",
};

function OAuth2RedirectContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const refreshToken = searchParams.get("refreshToken");
  const errorCode = searchParams.get("error");
  const linkError = errorCode ? (ERROR_MESSAGES[errorCode] ?? "소셜 로그인 처리 중 문제가 발생했어요.") : "";

  const socialLogin = useAuthStore((state) => state.socialLogin);

  useEffect(() => {
    if (errorCode) return;
    console.log("OAuth 리다이렉트 페이지 진입, 받은 토큰:", token);

    if (!token) {
      console.warn("토큰이 쿼리 스트링에 존재하지 않습니다!");
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        // 쿠키 저장 — 이메일은 여기서 안 심는다. JWT의 sub는 이제 사용자 ID라 이메일을 뽑을 수
        // 없고, socialLogin이 이 access_token으로 /api/users/me를 조회해 진짜 이메일을 채운다.
        document.cookie = `access_token=${token}; path=/; max-age=604800; SameSite=Lax`;
        if (refreshToken) {
          localStorage.setItem("refresh_token", refreshToken);
          document.cookie = `refresh_token=${refreshToken}; path=/; max-age=604800; SameSite=Lax`;
        }

        // 홈으로 넘어가기 전에 이메일 조회까지 기다려서, 헤더가 곧바로 정상 상태로 뜨게 한다.
        // 같은 조회 결과로 관리자 계정이면 로그인 직후 한 번만 대시보드로 보낸다 — 이후
        // "홈" 클릭은 일반 홈으로 간다.
        const me = await socialLogin(token, refreshToken);
        console.log("쿠키 및 스토어 저장 완료, 리다이렉트 대기 중...");
        if (!cancelled) window.location.href = me?.role === "ADMIN" ? "/admin/overview" : "/";
        return;
      } catch (e) {
        console.error("소셜 토큰 처리 실패:", e);
        return;
      }
    })();

    return () => { cancelled = true; };
  }, [token, refreshToken, errorCode, socialLogin]);

  if (linkError) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-6 text-center">
        <p className="text-lg font-medium text-red-600">{linkError}</p>
        <Link href="/" className="text-sm font-semibold text-blue-600 hover:underline">홈으로 돌아가기</Link>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center">
      <p className="text-lg font-medium text-slate-600">소셜 로그인 처리를 완료하고 있습니다...</p>
    </div>
  );
}

export default function OAuth2RedirectPage() {
  return (
    <Suspense fallback={<div className="flex min-h-screen items-center justify-center"><p>로딩 중...</p></div>}>
      <OAuth2RedirectContent />
    </Suspense>
  );
}