"use client";

import { useEffect, useState } from "react";
import { PencilSimple } from "@phosphor-icons/react";

export default function AccountSettings() {
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editingProfile, setEditingProfile] = useState(false);
  const [editingPassword, setEditingPassword] = useState(false);
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [refreshToken, setRefreshToken] = useState(0);

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

  if (loading) return <p className="text-sm text-slate-400">내 정보 확인 중...</p>;
  if (!me) return <p className="text-sm text-red-600">{error || "내 정보를 불러오지 못했습니다."}</p>;

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      {notice && <p className="mb-3 text-xs font-semibold text-emerald-600">{notice}</p>}

      {!editingProfile && !editingPassword && (
        <div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold text-slate-800">{me.name} · {me.nickname}</p>
              <p className="mt-0.5 text-xs text-slate-500">{me.email}</p>
              {me.regionName && <p className="mt-0.5 text-xs text-slate-400">{me.regionName}</p>}
            </div>
            <button
              type="button"
              onClick={startEditProfile}
              aria-label="개인정보 수정"
              className="rounded-full p-1.5 text-slate-400 hover:bg-slate-100"
            >
              <PencilSimple size={16} />
            </button>
          </div>
          <button
            type="button"
            onClick={startEditPassword}
            className="mt-3 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50"
          >
            비밀번호 변경
          </button>
        </div>
      )}

      {editingProfile && (
        <form onSubmit={handleProfileSubmit}>
          <p className="mb-3 text-sm font-bold text-slate-800">개인정보 수정</p>
          <label className="block text-xs font-semibold text-slate-500">이름</label>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={50}
            className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400"
          />
          <label className="mt-3 block text-xs font-semibold text-slate-500">닉네임</label>
          <input
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            maxLength={50}
            className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400"
          />
          {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
          <div className="mt-3 flex gap-2">
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-blue-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? "저장 중..." : "저장"}
            </button>
            <button
              type="button"
              onClick={() => setEditingProfile(false)}
              className="rounded-lg border border-slate-200 px-4 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50"
            >
              취소
            </button>
          </div>
        </form>
      )}

      {editingPassword && (
        <form onSubmit={handlePasswordSubmit}>
          <p className="mb-3 text-sm font-bold text-slate-800">비밀번호 변경</p>
          <label className="block text-xs font-semibold text-slate-500">현재 비밀번호</label>
          <input
            type="password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400"
          />
          <label className="mt-3 block text-xs font-semibold text-slate-500">새 비밀번호</label>
          <input
            type="password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            placeholder="영문+숫자 포함 8자 이상"
            className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400"
          />
          <label className="mt-3 block text-xs font-semibold text-slate-500">새 비밀번호 확인</label>
          <input
            type="password"
            value={newPasswordConfirm}
            onChange={(event) => setNewPasswordConfirm(event.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400"
          />
          {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
          <div className="mt-3 flex gap-2">
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-blue-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? "변경 중..." : "변경"}
            </button>
            <button
              type="button"
              onClick={() => setEditingPassword(false)}
              className="rounded-lg border border-slate-200 px-4 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50"
            >
              취소
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
