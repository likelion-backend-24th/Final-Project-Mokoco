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
      
      try {
        const base64Payload = token.split(".")[1];
        const payload = JSON.parse(atob(base64Payload));
        const email = payload.sub;
        if (email) {
          document.cookie = `user_email=${encodeURIComponent(email)}; path=/; max-age=604800; SameSite=Lax`;
          set({ accessToken: token, userEmail: email });
          return;
        }
      } catch (e) {
        console.error("소셜 토큰 파싱 실패", e);
      }
    }
    set({ accessToken: token });
  },

  setLogout: () => {
    if (typeof window !== "undefined") {
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
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
        try {
          const base64Payload = token.split(".")[1];
          const payload = JSON.parse(atob(base64Payload));
          set({ accessToken: token, userEmail: payload.sub });
        } catch (e) {
          console.error("Auth 복원 실패", e);
        }
      }
    }
  },
}));