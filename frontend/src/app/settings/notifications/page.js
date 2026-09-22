"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import SiteHeader from "@/components/site-header";

const FIELDS = [
  {
    key: "proposalReceived",
    title: "새 수리 제안 도착",
    desc: "내 수리 요청 게시글에 수리공이 제안을 보냈을 때 알림을 받습니다.",
  },
  {
    key: "proposalAdopted",
    title: "내 제안 채택",
    desc: "내가 보낸 수리 제안이 의뢰인에게 채택됐을 때 알림을 받습니다.",
  },
  {
    key: "chatMessage",
    title: "채팅 메시지",
    desc: "채팅방에서 상대방이 새 메시지를 보냈을 때 알림을 받습니다.",
  },
];

export default function NotificationSettingsPage() {
  const [settings, setSettings] = useState(null);
  const [status, setStatus] = useState("loading"); // loading | ready | saving
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let active = true;
    fetch("/api/notifications/settings", { cache: "no-store" })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "설정을 불러오지 못했습니다.");
        return data;
      })
      .then((data) => {
        if (!active) return;
        setSettings(data);
        setStatus("ready");
      })
      .catch((err) => {
        if (!active) return;
        setError(err.message);
        setStatus("ready");
        setSettings({ proposalReceived: true, proposalAdopted: true, chatMessage: true });
      });
    return () => {
      active = false;
    };
  }, []);

  async function toggle(key) {
    const next = { ...settings, [key]: !settings[key] };
    setSettings(next);
    setStatus("saving");
    setError("");
    try {
      const res = await fetch("/api/notifications/settings", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(next),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "저장에 실패했습니다.");
      setSettings(data);
      setSaved(true);
    } catch (err) {
      setError(err.message);
      setSettings((s) => ({ ...s, [key]: !next[key] })); // 롤백
    } finally {
      setStatus("ready");
    }
  }

  return (
    <>
      <SiteHeader />
      <main className="mx-auto max-w-2xl px-4 py-8">
        <nav className="mb-4 text-sm text-blue-600">
          <Link href="/posts">← 수리 요청 목록</Link>
        </nav>
        <h1 className="text-2xl font-bold text-slate-900">알림 설정</h1>
        <p className="mt-1 text-sm text-slate-500">
          받고 싶은 알림 종류를 선택하세요. 끄면 해당 알림은 저장되지 않고 실시간으로도 오지 않습니다.
        </p>

        {error && (
          <p role="alert" className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
            {error}
          </p>
        )}
        {saved && !error && (
          <p className="mt-4 text-sm text-green-600">저장되었습니다.</p>
        )}

        <ul className="mt-6 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
          {FIELDS.map((f) => (
            <li key={f.key} className="flex items-start justify-between gap-4 px-4 py-4">
              <div>
                <p className="text-sm font-semibold text-slate-800">{f.title}</p>
                <p className="mt-0.5 text-xs text-slate-500">{f.desc}</p>
              </div>
              <button
                type="button"
                role="switch"
                aria-checked={settings ? settings[f.key] : false}
                aria-label={f.title}
                disabled={!settings || status === "loading"}
                onClick={() => toggle(f.key)}
                className={`relative mt-0.5 h-6 w-11 shrink-0 rounded-full transition-colors ${
                  settings && settings[f.key] ? "bg-blue-600" : "bg-slate-300"
                } disabled:opacity-50`}
              >
                <span
                  className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition-all ${
                    settings && settings[f.key] ? "left-[22px]" : "left-0.5"
                  }`}
                />
              </button>
            </li>
          ))}
        </ul>
      </main>
    </>
  );
}
