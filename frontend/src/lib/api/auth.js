import { useAuthStore } from "@/store/authStore";

export async function loginUser(email, password) {
  const apiUrl = process.env.NEXT_PUBLIC_BACKEND_API_URL || "";
  
  const response = await fetch(`${apiUrl}/api/auth/signin`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || "로그인에 실패했습니다.");
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