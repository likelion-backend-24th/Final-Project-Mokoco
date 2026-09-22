import { useAuthStore } from "@/store/authStore";
import { backendUrl, readBackendPayload, errorMessage } from "@/lib/backend";

export async function loginUser(email, password) {
  const response = await fetch(backendUrl("/api/auth/signin"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  // 백엔드가 JSON이 아닌 본문(text/plain 등)을 내려도 response.json()처럼 그대로
  // 터지지 않고 안전하게 파싱한다 — 실패 메시지가 "... is not valid JSON"으로 뜨던 문제.
  const data = await readBackendPayload(response);
  if (!response.ok) {
    throw new Error(errorMessage(data, "로그인에 실패했습니다."));
  }

  if (data.accessToken) {
    // Refresh Token은 localStorage에 백업용으로 보관 (선택 사항)
    if (data.refreshToken && typeof window !== "undefined") {
      localStorage.setItem("refresh_token", data.refreshToken);
    }

    // accessToken의 sub는 이제 이메일이 아니라 사용자 ID다 — 로그인 폼에서 받은 진짜
    // 이메일(email 파라미터)을 그대로 쓴다.
    useAuthStore.getState().setLogin(
      data.accessToken,
      email,
      data.regionCode,
      data.regionName
    );
  }

  return data;
}

export async function logoutUser() {
  try {
    const apiUrl = process.env.NEXT_PUBLIC_BACKEND_API_URL || "";
    await fetch(`${apiUrl}/api/auth/logout`, { method: "POST" });
  } catch (e) {
    console.error("로그아웃 통신 실패", e);
  } finally {
    if (typeof window !== "undefined") {
      localStorage.removeItem("refresh_token");
    }
    
    useAuthStore.getState().setLogout();
    window.location.href = "/";
  }
}