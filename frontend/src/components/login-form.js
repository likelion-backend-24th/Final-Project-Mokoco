"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Eye, EyeSlash, SignIn } from "@phosphor-icons/react";

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
    try {
      const response = await fetch("/api/auth/signin", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: formData.get("email"), password: formData.get("password") }),
      });
      const payload = await response.json();
      if (!response.ok) { setMessage(payload.message ?? "로그인에 실패했습니다."); return; }
      router.push("/");
      router.refresh();
    } catch { setMessage("서버와 통신할 수 없습니다. 잠시 후 다시 시도해주세요."); }
    finally { setLoading(false); }
  }

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
      <p className="mt-7 border-t border-slate-200 pt-6 text-center text-sm text-slate-500">처음이신가요? <Link href="/signup" className="font-bold text-blue-600 hover:underline">회원가입</Link></p>
    </section>
  );
}
