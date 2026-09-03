"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Eye, EyeSlash, SignIn } from "@phosphor-icons/react";
import { loginUser } from "@/lib/api/auth";

export default function LoginForm({ registered = false }) {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    const formData = new FormData(event.currentTarget);
    const email = formData.get("email");
    const password = formData.get("password");

    try {
      // 서버 API 라우트(/api/auth/signin)를 통해 백엔드 통신 및 HttpOnly 쿠키 세팅 수행
      await loginUser(email, password);

      // 클라이언트 라우터 캐시 우회를 위해 강제 새로고침 이동 적용
      window.location.href = "/";
    } catch (err) {
      setMessage(err.message || "이메일 또는 비밀번호가 올바르지 않습니다.");
    } finally {
      setLoading(false);
    }
  }

  const handleSocialLogin = (provider) => {
    const backendBaseUrl = process.env.NEXT_PUBLIC_BACKEND_API_URL || "http://localhost:8000";
    window.location.href = `${backendBaseUrl}/oauth2/authorization/${provider}`;
  };

  return (
    <section className="auth-card">
      <div className="flex items-center gap-4">
        <span className="grid size-12 place-items-center rounded-2xl bg-blue-50 text-blue-600"><SignIn size={28} weight="duotone" /></span>
        <div><h2>로그인</h2><p>우리 동네 이웃과 수리를 나눠보세요.</p></div>
      </div>
      <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
        {registered && (
          <p className="form-message form-message-success" role="status">
            회원가입이 완료되었습니다. 등록한 계정으로 로그인해주세요.
          </p>
        )}
        <label className="form-field"><span>이메일</span><input type="email" name="email" autoComplete="email" placeholder="이메일을 입력하세요" required /></label>
        <label className="form-field"><span>비밀번호</span><div className="password-field"><input type={showPassword ? "text" : "password"} name="password" autoComplete="current-password" placeholder="비밀번호를 입력하세요" required /><button type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 보기"}>{showPassword ? <EyeSlash size={21} /> : <Eye size={21} />}</button></div></label>
        {message && <p className="form-message form-message-error" role="alert">{message}</p>}
        <button className="primary-button h-14 w-full justify-center text-base" type="submit" disabled={loading}>{loading ? "로그인 중..." : "로그인"}</button>
      </form>

      <div className="mt-6">
        <div className="relative flex py-2 items-center">
          <div className="flex-grow border-t border-slate-200"></div>
          <span className="flex-shrink mx-4 text-xs text-slate-400">소셜 계정으로 간편 로그인</span>
          <div className="flex-grow border-t border-slate-200"></div>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={() => handleSocialLogin("google")}
            className="flex h-12 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
          >
            <svg className="size-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"/>
              <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.13 0-5.78-2.11-6.73-4.96H1.18v3.15C3.15 21.32 7.21 24 12 24z"/>
              <path fill="#FBBC05" d="M5.27 14.24c-.25-.72-.38-1.49-.38-2.24s.13-1.52.38-2.24V6.61H1.18C.43 8.13 0 9.87 0 11.7s.43 3.57 1.18 5.09l4.09-2.55z"/>
              <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.21 0 3.15 2.68 1.18 6.61l4.09 3.15c.95-2.85 3.6-4.96 6.73-4.96z"/>
            </svg>
            구글 로그인
          </button>

          <button
            type="button"
            onClick={() => handleSocialLogin("kakao")}
            className="flex h-12 items-center justify-center gap-2 rounded-xl bg-[#FEE500] px-4 text-sm font-semibold text-[#191919] hover:bg-[#FDD835] transition-colors"
          >
            <svg className="size-5" viewBox="0 0 24 24" fill="#191919">
              <path d="M12 3C6.48 3 2 6.58 2 11c0 2.84 1.83 5.32 4.58 6.72-.16.59-.59 2.15-.68 2.48-.11.41.15.4.32.28.13-.09 2.05-1.39 2.87-1.95.62.09 1.26.14 1.91.14 5.52 0 10-3.58 10-8s-4.48-8-10-8z"/>
            </svg>
            카카오 로그인
          </button>
        </div>
      </div>

      <p className="mt-7 border-t border-slate-200 pt-6 text-center text-sm text-slate-500">처음이신가요? <Link href="/signup" className="font-bold text-blue-600 hover:underline">회원가입</Link></p>
    </section>
  );
}