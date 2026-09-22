"use client";

import { useEffect, useState } from "react";
import { PencilSimple, LockKey, FileText, X, Link as LinkIcon } from "@phosphor-icons/react";
import ResumeEditor from "./resume-editor";

const SOCIAL_PROVIDERS = [
  { id: "google", label: "구글" },
  { id: "kakao", label: "카카오" },
];

// 로그인 페이지의 소셜 버튼과 같은 경로다 — 이미 로그인된 상태에서 타면 백엔드가
// "새 로그인"이 아니라 "지금 계정에 연결"로 처리한다(CustomOAuth2UserService 참고).
function navigateToOAuth(providerId) {
  window.location.href = `/oauth2/authorization/${providerId}`;
}

export default function AccountSettings() {
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editingProfile, setEditingProfile] = useState(false);
  const [editingPassword, setEditingPassword] = useState(false);
  const [resumeOpen, setResumeOpen] = useState(false);
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [refreshToken, setRefreshToken] = useState(0);
  const [linkedProviders, setLinkedProviders] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/users/me", { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error ?? "내 정보를 불러오지 못했습니다.");
        setMe(json);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [refreshToken]);

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/users/me/social-accounts", { signal: controller.signal, cache: "no-store" })
      .then((res) => (res.ok ? res.json() : []))
      .then((providers) => setLinkedProviders(Array.isArray(providers) ? providers : []))
      .catch(() => {});
    return () => controller.abort();
  }, [refreshToken]);

  function startEditProfile() {
    setName(me?.name ?? "");
    setNickname(me?.nickname ?? "");
    setError("");
    setNotice("");
    setEditingProfile(true);
  }

  async function handleProfileSubmit(event) {
    event.preventDefault();
    if (!name.trim() || !nickname.trim()) {
      setError("이름과 닉네임을 모두 입력해주세요.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const res = await fetch("/api/users/me", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, nickname }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error ?? "정보를 수정하지 못했습니다.");
        return;
      }
      setEditingProfile(false);
      const changed = [];
      if (name !== me.name) changed.push("이름");
      if (nickname !== me.nickname) changed.push("닉네임");
      if (changed.length > 0) setNotice(`${changed.join(", ")}이(가) 변경되었습니다.`);
      setRefreshToken((token) => token + 1);
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  function startEditPassword() {
    setCurrentPassword("");
    setNewPassword("");
    setNewPasswordConfirm("");
    setError("");
    setNotice("");
    setEditingPassword(true);
  }

  async function handlePasswordSubmit(event) {
    event.preventDefault();
    if (!currentPassword || !newPassword) {
      setError("현재 비밀번호와 새 비밀번호를 입력해주세요.");
      return;
    }
    if (newPassword !== newPasswordConfirm) {
      setError("새 비밀번호가 서로 일치하지 않아요.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const res = await fetch("/api/users/me/password", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error ?? "비밀번호를 변경하지 못했습니다.");
        return;
      }
      setEditingPassword(false);
      setNotice("비밀번호가 변경됐어요.");
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <p className="text-base text-slate-400">내 정보 확인 중...</p>;
  if (!me) return <p className="text-base text-red-600">{error || "내 정보를 불러오지 못했습니다."}</p>;

  return (
    <>
    <div className="rounded-xl border border-slate-200 bg-white p-6">
      {notice && <p className="mb-4 text-sm font-semibold text-emerald-600">{notice}</p>}

      {!editingProfile && !editingPassword && (
        <div>
          <div>
            <p className="text-lg font-bold text-slate-800">{me.name} · {me.nickname}</p>
            <p className="mt-1 text-sm text-slate-500">{me.email}</p>
            {me.regionName && <p className="mt-0.5 text-sm text-slate-400">{me.regionName}</p>}
          </div>
          <div className="mt-4 flex gap-3">
            <button
              type="button"
              onClick={startEditProfile}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
            >
              <PencilSimple size={16} weight="bold" />
              정보 수정
            </button>
            <button
              type="button"
              onClick={startEditPassword}
              className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-500 hover:bg-slate-50"
            >
              <LockKey size={16} weight="bold" />
              비밀번호 변경
            </button>
            <button
              type="button"
              onClick={() => setResumeOpen(true)}
              className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-500 hover:bg-slate-50"
            >
              <FileText size={16} weight="bold" />
              내 이력서
            </button>
          </div>

          <div className="mt-6 border-t border-slate-100 pt-4">
            <p className="text-sm font-bold text-slate-700">연결된 소셜 계정</p>
            <p className="mt-1 text-xs text-slate-400">
              두 계정 모두 본인 소유임을 증명해야 연결돼요. 이미 다른 계정에 연결된 소셜 계정은 연결할 수 없어요.
            </p>
            <div className="mt-3 flex flex-col gap-2">
              {SOCIAL_PROVIDERS.map(({ id, label }) => {
                const linked = linkedProviders?.includes(id.toUpperCase());
                return (
                  <div key={id} className="flex items-center justify-between rounded-lg border border-slate-200 px-4 py-2.5">
                    <span className="text-sm font-medium text-slate-600">{label}</span>
                    {linked ? (
                      <span className="text-xs font-semibold text-emerald-600">연결됨</span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => navigateToOAuth(id)}
                        disabled={linkedProviders === null}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50 disabled:opacity-50"
                      >
                        <LinkIcon size={14} weight="bold" />
                        연결하기
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {editingProfile && (
        <form onSubmit={handleProfileSubmit}>
          <p className="mb-4 text-base font-bold text-slate-800">개인정보 수정</p>
          <label className="block text-sm font-semibold text-slate-500">이름</label>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={50}
            className="mt-1.5 w-full rounded-lg border border-slate-200 px-4 py-2.5 text-base outline-none focus:border-blue-400"
          />
          <label className="mt-4 block text-sm font-semibold text-slate-500">닉네임</label>
          <input
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            maxLength={50}
            className="mt-1.5 w-full rounded-lg border border-slate-200 px-4 py-2.5 text-base outline-none focus:border-blue-400"
          />
          {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
          <div className="mt-4 flex gap-3">
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? "저장 중..." : "저장"}
            </button>
            <button
              type="button"
              onClick={() => setEditingProfile(false)}
              className="rounded-lg border border-slate-200 px-5 py-2 text-sm font-semibold text-slate-500 hover:bg-slate-50"
            >
              취소
            </button>
          </div>
        </form>
      )}

      {editingPassword && (
        <form onSubmit={handlePasswordSubmit}>
          <p className="mb-4 text-base font-bold text-slate-800">비밀번호 변경</p>
          <label className="block text-sm font-semibold text-slate-500">현재 비밀번호</label>
          <input
            type="password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            className="mt-1.5 w-full rounded-lg border border-slate-200 px-4 py-2.5 text-base outline-none focus:border-blue-400"
          />
          <label className="mt-4 block text-sm font-semibold text-slate-500">새 비밀번호</label>
          <input
            type="password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            placeholder="영문+숫자 포함 8자 이상"
            className="mt-1.5 w-full rounded-lg border border-slate-200 px-4 py-2.5 text-base outline-none focus:border-blue-400"
          />
          <label className="mt-4 block text-sm font-semibold text-slate-500">새 비밀번호 확인</label>
          <input
            type="password"
            value={newPasswordConfirm}
            onChange={(event) => setNewPasswordConfirm(event.target.value)}
            className="mt-1.5 w-full rounded-lg border border-slate-200 px-4 py-2.5 text-base outline-none focus:border-blue-400"
          />
          {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
          <div className="mt-4 flex gap-3">
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? "변경 중..." : "변경"}
            </button>
            <button
              type="button"
              onClick={() => setEditingPassword(false)}
              className="rounded-lg border border-slate-200 px-5 py-2 text-sm font-semibold text-slate-500 hover:bg-slate-50"
            >
              취소
            </button>
          </div>
        </form>
      )}
    </div>

    {resumeOpen && (
      <div
        className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm transition-opacity"
        onClick={() => setResumeOpen(false)}
      />
    )}

    <div
      className={`fixed inset-x-0 bottom-0 z-50 max-h-[85vh] overflow-y-auto rounded-t-3xl bg-white p-6 shadow-2xl transition-all duration-300 ease-out md:inset-0 md:m-auto md:h-fit md:w-full md:max-w-lg md:rounded-3xl ${
        resumeOpen
          ? "translate-y-0 md:translate-y-0 md:visible md:opacity-100"
          : "translate-y-full md:translate-y-0 md:invisible md:opacity-0"
      }`}
    >
      <div className="mx-auto mb-4 h-1.5 w-12 rounded-full bg-slate-200 md:hidden" />

      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-lg font-extrabold text-slate-900">내 이력서</h3>
        <button
          type="button"
          onClick={() => setResumeOpen(false)}
          aria-label="닫기"
          className="rounded-full p-1 text-slate-400 hover:bg-slate-100"
        >
          <X size={20} weight="bold" />
        </button>
      </div>

      <ResumeEditor />
    </div>
    </>
  );
}
