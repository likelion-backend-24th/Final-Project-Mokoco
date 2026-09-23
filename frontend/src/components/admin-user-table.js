"use client";

import { useState } from "react";
import { ShieldCheck, ArrowsLeftRight, ProhibitInset, ArrowCounterClockwise } from "@phosphor-icons/react";

const STATUS_FILTERS = [
  { value: "ALL", label: "전체" },
  { value: "ACTIVE", label: "활성" },
  { value: "SUSPENDED", label: "정지" },
];

export default function AdminUserTable({ initialUsers, currentUserEmail }) {
  const [users, setUsers] = useState(initialUsers);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loadingId, setLoadingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function toggleRole(user) {
    const nextRole = user.role === "ADMIN" ? "USER" : "ADMIN";
    if (!confirm(`${user.email}의 권한을 ${nextRole}(으)로 변경하시겠습니까?`)) return;
    setLoadingId(user.id);
    setError("");
    setNotice("");
    try {
      const response = await fetch(`/api/admin/users/${user.id}/role`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ role: nextRole }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "권한 변경에 실패했습니다.");
      setUsers((current) => current.map((u) => (u.id === user.id ? { ...u, role: nextRole } : u)));
      setNotice(`${user.email}의 권한을 ${nextRole}(으)로 변경했어요.`);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoadingId(null);
    }
  }

  async function toggleStatus(user) {
    const nextStatus = user.status === "SUSPENDED" ? "ACTIVE" : "SUSPENDED";
    const verb = nextStatus === "SUSPENDED" ? "정지" : "정지 해제";
    if (!confirm(`${user.email} 계정을 ${verb}하시겠습니까?`)) return;
    setLoadingId(user.id);
    setError("");
    setNotice("");
    try {
      const response = await fetch(`/api/admin/users/${user.id}/status`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: nextStatus }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "상태 변경에 실패했습니다.");
      setUsers((current) => current.map((u) => (u.id === user.id ? { ...u, status: nextStatus } : u)));
      setNotice(`${user.email} 계정을 ${verb}했어요.`);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoadingId(null);
    }
  }

  const visibleUsers = statusFilter === "ALL" ? users : users.filter((user) => user.status === statusFilter);

  return (
    <div className="dashboard-card">
      <div className="mb-4 flex items-center gap-3">
        <select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-600 outline-none focus:border-blue-400"
        >
          {STATUS_FILTERS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
        <span className="text-xs text-slate-400">총 {visibleUsers.length.toLocaleString("ko-KR")}명</span>
      </div>

      {notice && <p role="status" className="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{notice}</p>}
      {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>}

      {visibleUsers.length === 0 ? (
        <p className="py-10 text-center text-sm text-slate-400">해당하는 회원이 없어요.</p>
      ) : (
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-xs font-semibold uppercase tracking-wide text-slate-400">
              <th className="py-3 pr-4">이메일</th>
              <th className="py-3 pr-4">닉네임</th>
              <th className="py-3 pr-4">권한</th>
              <th className="py-3 pr-4">상태</th>
              <th className="py-3 pr-4">가입일</th>
              <th className="py-3 pr-4"></th>
            </tr>
          </thead>
          <tbody>
            {visibleUsers.map((user) => {
              const isSelf = currentUserEmail && user.email === currentUserEmail;
              const isAdmin = user.role === "ADMIN";
              return (
                <tr key={user.id} className="border-b border-slate-100 last:border-0">
                  <td className="py-3 pr-4 font-medium text-slate-800">{user.email}</td>
                  <td className="py-3 pr-4 text-slate-600">{user.nickname}</td>
                  <td className="py-3 pr-4">
                    <span
                      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-bold ${
                        isAdmin ? "bg-blue-100 text-blue-700" : "bg-slate-100 text-slate-600"
                      }`}
                    >
                      {isAdmin && <ShieldCheck size={13} weight="bold" />}
                      {user.role}
                    </span>
                  </td>
                  <td className="py-3 pr-4">
                    <span
                      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold ${
                        user.status === "SUSPENDED" ? "bg-red-100 text-red-700" : "bg-emerald-100 text-emerald-700"
                      }`}
                    >
                      {user.status === "SUSPENDED" ? "정지" : "활성"}
                    </span>
                  </td>
                  <td className="py-3 pr-4 text-xs text-slate-400">
                    {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : ""}
                  </td>
                  <td className="py-3 pr-4">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => toggleRole(user)}
                        disabled={isSelf || loadingId !== null}
                        title={isSelf ? "본인 권한은 여기서 변경할 수 없습니다." : undefined}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-300 transition disabled:opacity-40"
                      >
                        <ArrowsLeftRight size={14} weight="bold" />
                        {loadingId === user.id ? "변경 중..." : isAdmin ? "USER로" : "ADMIN으로"}
                      </button>
                      <button
                        type="button"
                        onClick={() => toggleStatus(user)}
                        disabled={isSelf || loadingId !== null}
                        title={isSelf ? "본인 계정은 여기서 정지할 수 없습니다." : undefined}
                        className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition disabled:opacity-40 ${
                          user.status === "SUSPENDED" ? "bg-emerald-100 text-emerald-700 hover:bg-emerald-200" : "bg-red-100 text-red-700 hover:bg-red-200"
                        }`}
                      >
                        {user.status === "SUSPENDED" ? <ArrowCounterClockwise size={14} weight="bold" /> : <ProhibitInset size={14} weight="bold" />}
                        {loadingId === user.id ? "처리 중..." : user.status === "SUSPENDED" ? "정지 해제" : "정지"}
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      )}
    </div>
  );
}
