import { useAuthStore } from "@/store/authStore";

export async function loginUser(email, password) {
  const response = await fetch("/api/auth/signin", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || "로그인에 실패했습니다.");
  }

  if (data.accessToken) {
    useAuthStore.getState().setLogin(data.accessToken, email);
    if (data.refreshToken) {
      // 로컬스토리지와 일반 쿠키 양쪽에 refresh_token을 확실하게 동기화
      localStorage.setItem("refresh_token", data.refreshToken);
      document.cookie = `refresh_token=${data.refreshToken}; path=/; max-age=604800; SameSite=Lax`;
    }
  }

  return data;
}

export async function logoutUser() {
  const response = await fetch("/api/auth/logout", {
    method: "POST",
  });
  
  useAuthStore.getState().setLogout();
  return response.ok;
}