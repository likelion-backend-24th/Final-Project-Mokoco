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
    let userEmail = email;

    // accessToken 페이로드에서 sub(이메일) 추출
    try {
      const base64Payload = data.accessToken.split(".")[1];
      const jsonPayload = decodeURIComponent(
        atob(base64Payload)
          .split("")
          .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
          .join("")
      );
      const payload = JSON.parse(jsonPayload);
      userEmail = payload.sub || email;
    } catch (e) {
      console.error("토큰 파싱 실패", e);
    }

    // Refresh Token은 localStorage에 백업용으로 보관 (선택 사항)
    if (data.refreshToken && typeof window !== "undefined") {
      localStorage.setItem("refresh_token", data.refreshToken);
    }

    // Zustand 스토어 업데이트
    useAuthStore.getState().setLogin(
      data.accessToken, 
      userEmail, 
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