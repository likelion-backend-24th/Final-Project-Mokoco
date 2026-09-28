// store/authStore.js
import { create } from "zustand";

export const useAuthStore = create((set) => ({
  accessToken: null,
  userEmail: null,
  
  setLogin: (token, email) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("access_token", token);
      document.cookie = `access_token=${token}; path=/; max-age=604800; SameSite=Lax`;
      if (email) {
        document.cookie = `user_email=${encodeURIComponent(email)}; path=/; max-age=604800; SameSite=Lax`;
      }
    }
    set({ accessToken: token, userEmail: email });
  },

  socialLogin: (token, refreshToken) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("access_token", token);
      document.cookie = `access_token=${token}; path=/; max-age=604800; SameSite=Lax`;

      if (refreshToken) {
        localStorage.setItem("refresh_token", refreshToken);
        document.cookie = `refresh_token=${refreshToken}; path=/; max-age=604800; SameSite=Lax`;
      }

      set({ accessToken: token });

      // JWT의 sub는 이제 이메일이 아니라 사용자 ID라 토큰에서 이메일을 뽑을 수 없다 —
      // 방금 심어둔 access_token 쿠키로 내 정보를 조회해서 진짜 이메일을 받아온다.
      // 호출부(oauth2/redirect)가 리다이렉트 전에 이 완료를 기다릴 수 있도록 promise를 반환한다.
      return fetch("/api/users/me", { cache: "no-store" })
        .then((res) => (res.ok ? res.json() : null))
        .then((me) => {
          if (!me?.email) return me;
          document.cookie = `user_email=${encodeURIComponent(me.email)}; path=/; max-age=604800; SameSite=Lax`;
          set({ userEmail: me.email });
          return me;
        })
        .catch((e) => { console.error("소셜 로그인 이메일 조회 실패", e); return null; });
    }
    set({ accessToken: token });
  },

  setLogout: () => {
    if (typeof window !== "undefined") {
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
      // 헤더가 캐시해둔 닉네임 — 안 지우면 같은 브라우저에서 다른 계정으로 로그인할 때
      // 이전 사용자의 닉네임이 잠깐 스쳐 지나간다.
      localStorage.removeItem("nickname");
      // access_token/user_email/refresh_token은 서버가 httpOnly로 심어둬서 document.cookie로는
      // 지워지지 않는다(같은 이름의 httpOnly 쿠키가 있으면 JS의 쓰기 자체가 브라우저에서 조용히
      // 무시된다) — 토큰 재발급 실패 시 AuthInitializer가 이 함수를 서버 라우트 없이 바로 부르는
      // 경로가 있어서, 그때 로그아웃 API를 안 타면 httpOnly 쿠키가 만료 전까지 그대로 남아있었다.
      // 그 상태로 탭을 새로고침하거나 다시 들어오면 서버 렌더링은 여전히 그 쿠키를 보고 로그인된
      // 것처럼 그려서, 화면이 로그인/로그아웃을 오가는 것처럼 보였다 — 여기서 항상 정리한다.
      fetch("/api/auth/logout", { method: "POST" }).catch(() => {});
    }
    document.cookie = "user_email=; path=/; max-age=0;";
    document.cookie = "access_token=; path=/; max-age=0;";
    document.cookie = "refresh_token=; path=/; max-age=0;";
    set({ accessToken: null, userEmail: null });
  },

  initAuth: () => {
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("access_token");
      if (token) {
        document.cookie = `access_token=${token}; path=/; max-age=604800; SameSite=Lax`;
        // user_email 쿠키는 로그인/재발급 시 서버가 이미 정확히 심어둔다 — JWT의 sub는
        // 이제 사용자 ID라 여기서 다시 뽑아 덮어쓰면 이메일 자리에 ID가 들어가 버린다.
        set({ accessToken: token });
      }
    }
  },
}));