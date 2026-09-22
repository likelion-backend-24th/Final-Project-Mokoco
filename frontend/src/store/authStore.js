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
          if (!me?.email) return;
          document.cookie = `user_email=${encodeURIComponent(me.email)}; path=/; max-age=604800; SameSite=Lax`;
          set({ userEmail: me.email });
        })
        .catch((e) => console.error("소셜 로그인 이메일 조회 실패", e));
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