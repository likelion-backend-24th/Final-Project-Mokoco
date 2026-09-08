// app/oauth2/redirect/page.jsx 수정
"use client";

import { useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { jwtDecode } from "jwt-decode"; // 💡 jwt-decode 임포트

function OAuth2RedirectContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const refreshToken = searchParams.get("refreshToken");
  
  const socialLogin = useAuthStore((state) => state.socialLogin);

  useEffect(() => {
    console.log("OAuth 리다이렉트 페이지 진입, 받은 토큰:", token);

    if (token) {
      try {
        // 💡 jwt-decode를 사용하여 안전하게 페이로드 추출
        const payload = jwtDecode(token);
        
        const email = payload.sub || "";
        console.log("추출된 이메일:", email);

        // 쿠키 저장
        document.cookie = `access_token=${token}; path=/; max-age=604800; SameSite=Lax`;
        if (refreshToken) {
          localStorage.setItem("refresh_token", refreshToken);
          document.cookie = `refresh_token=${refreshToken}; path=/; max-age=604800; SameSite=Lax`;
        }
        if (email) {
          document.cookie = `user_email=${encodeURIComponent(email)}; path=/; max-age=604800; SameSite=Lax`;
        }

        socialLogin(token, refreshToken);
        console.log("쿠키 및 스토어 저장 완료, 리다이렉트 대기 중...");
      } catch (e) {
        console.error("소셜 토큰 처리 실패:", e);
        return;
      }

      const timer = setTimeout(() => {
        window.location.href = "/";
      }, 300);

      return () => clearTimeout(timer);
    } else {
      console.warn("토큰이 쿼리 스트링에 존재하지 않습니다!");
    }
  }, [token, refreshToken, socialLogin]);

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