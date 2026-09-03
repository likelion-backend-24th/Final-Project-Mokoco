"use client";

import { useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useAuthStore } from "@/store/authStore";

function OAuth2RedirectContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const refreshToken = searchParams.get("refreshToken");
  
  // socialLogin 액션 가져오기
  const socialLogin = useAuthStore((state) => state.socialLogin);

  useEffect(() => {
    if (token) {
      try {
        // 소셜 로그인 전용 액션 호출 (access_token, refresh_token, user_email 일괄 처리)
        socialLogin(token, refreshToken);
      } catch (e) {
        console.error("소셜 토큰 처리 실패", e);
        window.location.href = "/login";
        return;
      }

      // 메인으로 강제 이동
      window.location.href = "/";
    }
  }, [token, refreshToken, socialLogin]);

  return (
    <div className="flex min-h-screen items-center justify-center">
      <p className="text-lg font-medium text-slate-600">로그인 처리를 완료하고 있습니다...</p>
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