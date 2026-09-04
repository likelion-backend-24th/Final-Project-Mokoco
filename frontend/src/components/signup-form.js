"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Eye, EyeSlash } from "@phosphor-icons/react";
import TermsModal from "@/components/terms-modal";

export default function SignupForm() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [agreed, setAgreed] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setMessage("");
    const formData = new FormData(event.currentTarget);
    const password = formData.get("password");
    if (password !== formData.get("passwordConfirm")) { setMessage("비밀번호가 일치하지 않습니다."); return; }
    setLoading(true);
    try {
      const response = await fetch("/api/auth/signup", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: formData.get("name"), nickname: formData.get("nickname"), email: formData.get("email"), password, regionCode: null }),
      });
      const payload = await response.json();
      if (!response.ok) { setMessage(payload.message ?? "회원가입에 실패했습니다."); return; }
      router.push("/login?registered=true");
    } catch { setMessage("서버와 통신할 수 없습니다. 잠시 후 다시 시도해주세요."); }
    finally { setLoading(false); }
  }

  return (
    <section className="auth-card">
      <div><h2>회원가입</h2><p>필수 정보를 입력하고 가까운 이웃과 연결해보세요.</p></div>
      <form className="mt-7 grid gap-5 sm:grid-cols-2" onSubmit={handleSubmit}>
        <label className="form-field"><span>이름</span><input name="name" autoComplete="name" placeholder="이름을 입력해주세요" required /></label>
        <label className="form-field"><span>닉네임</span><input name="nickname" autoComplete="nickname" placeholder="동네에서 사용할 이름" required /></label>
        <label className="form-field sm:col-span-2"><span>이메일</span><input type="email" name="email" autoComplete="email" placeholder="이메일 주소를 입력해주세요" required /></label>
        <label className="form-field sm:col-span-2"><span>비밀번호</span><div className="password-field"><input type={showPassword ? "text" : "password"} name="password" autoComplete="new-password" minLength={8} placeholder="영문과 숫자를 포함해 8자 이상" required /><button type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 보기"}>{showPassword ? <EyeSlash size={21} /> : <Eye size={21} />}</button></div></label>
        <label className="form-field sm:col-span-2"><span>비밀번호 확인</span><input type={showPassword ? "text" : "password"} name="passwordConfirm" autoComplete="new-password" minLength={8} placeholder="비밀번호를 다시 입력해주세요" required /></label>
        <label className="sm:col-span-2 flex items-start gap-3 text-sm leading-6 text-slate-600"><input type="checkbox" name="agreement" checked={agreed} onChange={(event) => setAgreed(event.target.checked)} className="mt-1 size-4 accent-blue-600" required /><span><TermsModal trigger="이용약관 및 개인정보 수집" onConfirm={() => setAgreed(true)} />에 동의합니다.</span></label>
        {message && <p className="form-message form-message-error sm:col-span-2" role="alert">{message}</p>}
        <button className="primary-button h-14 justify-center text-base sm:col-span-2" type="submit" disabled={loading}>{loading ? "가입 중..." : "회원가입"}</button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">이미 계정이 있나요? <Link href="/login" className="font-bold text-blue-600 hover:underline">로그인</Link></p>
    </section>
  );
}
