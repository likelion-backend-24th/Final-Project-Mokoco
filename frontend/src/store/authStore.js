import { create } from "zustand";
import { userIdFromToken } from "@/lib/user-id";

function storeToken(token) {
  localStorage.setItem("access_token", token);
  document.cookie = `access_token=${token}; path=/; max-age=1800; SameSite=Lax`;
}
async function currentEmail() {
  const response = await fetch("/api/users/me", { cache: "no-store" });
  if (!response.ok) return null;
  const user = await response.json();
  return user.email ?? null;
}
export const useAuthStore = create((set) => ({
  accessToken: null, userId: null, userEmail: null,
  setLogin: (token, email) => {
    storeToken(token);
    if (email) document.cookie = `user_email=${encodeURIComponent(email)}; path=/; max-age=1800; SameSite=Lax`;
    set({ accessToken: token, userId: userIdFromToken(token), userEmail: email });
  },
  socialLogin: async (token, refreshToken) => {
    storeToken(token);
    if (refreshToken) {
      localStorage.setItem("refresh_token", refreshToken);
      document.cookie = `refresh_token=${refreshToken}; path=/; max-age=604800; SameSite=Lax`;
    }
    const email = await currentEmail().catch(() => null);
    if (!email) throw new Error("사용자 정보를 확인할 수 없습니다.");
    document.cookie = `user_email=${encodeURIComponent(email)}; path=/; max-age=1800; SameSite=Lax`;
    set({ accessToken: token, userId: userIdFromToken(token), userEmail: email });
  },
  setLogout: () => {
    localStorage.removeItem("access_token"); localStorage.removeItem("refresh_token");
    for (const key of ["user_email", "access_token", "refresh_token"]) document.cookie = `${key}=; path=/; max-age=0;`;
    set({ accessToken: null, userId: null, userEmail: null });
  },
  initAuth: async () => {
    const token = localStorage.getItem("access_token");
    if (!token) return;
    const email = await currentEmail().catch(() => null);
    set({ accessToken: token, userId: userIdFromToken(token), userEmail: email });
  },
}));
