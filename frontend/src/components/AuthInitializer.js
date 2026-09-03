// components/AuthInitializer.js
"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/store/authStore";
import { useRouter } from "next/navigation";

export default function AuthInitializer() {
  const initAuth = useAuthStore((state) => state.initAuth);
  const router = useRouter();

  useEffect(() => {
    initAuth();

    // 브라우저 뒤로가기 등으로 bfcache(캐시)에서 페이지가 살아날 때 실행
    const handlePageShow = (event) => {
      if (event.persisted) {
        router.refresh(); // 서버 컴포넌트와 쿠키 상태를 강제로 다시 동기화
      }
    };

    window.addEventListener("pageshow", handlePageShow);
    return () => {
      window.removeEventListener("pageshow", handlePageShow);
    };
  }, [initAuth, router]);

  return null;
}