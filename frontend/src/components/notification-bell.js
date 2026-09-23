"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Bell, Gear } from "@phosphor-icons/react/dist/ssr";
import { useNotificationStore } from "@/store/notificationStore";

function relativeTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const minutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  return `${Math.floor(hours / 24)}일 전`;
}

function targetHref(n) {
  if (n.type === "CHAT_MESSAGE" && n.chatRoomId) return `/chat-rooms/${n.chatRoomId}`;
  if (n.postId) return `/posts/${n.postId}`;
  return null;
}

export default function NotificationBell() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const { items, unreadCount, markRead, markAllRead } = useNotificationStore();

  useEffect(() => {
    if (!open) return;
    const onClick = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, [open]);

  async function handleSelect(n) {
    setOpen(false);
    if (!n.isRead) {
      markRead(n.id);
      fetch(`/api/notifications/${n.id}/read`, { method: "PATCH" }).catch(() => {});
    }
    const href = targetHref(n);
    if (href) router.push(href);
  }

  async function handleReadAll() {
    markAllRead();
    fetch("/api/notifications/read-all", { method: "PATCH" }).catch(() => {});
  }

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-label={`알림${unreadCount ? ` (안 읽음 ${unreadCount}개)` : ""}`}
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className="relative flex h-9 w-9 items-center justify-center rounded-full text-slate-600 hover:bg-slate-100"
      >
        <Bell size={20} weight={unreadCount ? "fill" : "regular"} />
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold leading-4 text-white">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="fixed inset-x-3 top-16 z-50 max-h-[75vh] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg sm:absolute sm:inset-x-auto sm:top-auto sm:right-0 sm:mt-2 sm:w-80 sm:max-w-[92vw] sm:max-h-none">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
            <span className="text-sm font-semibold text-slate-800">알림</span>
            <div className="flex items-center gap-3">
              {unreadCount > 0 && (
                <button type="button" onClick={handleReadAll} className="text-xs text-blue-600 hover:underline">
                  모두 읽음
                </button>
              )}
              <Link
                href="/settings/notifications"
                onClick={() => setOpen(false)}
                aria-label="알림 설정"
                className="text-slate-400 hover:text-slate-600"
              >
                <Gear size={16} />
              </Link>
            </div>
          </div>

          <ul className="max-h-96 overflow-y-auto">
            {items.length === 0 && (
              <li className="px-4 py-10 text-center text-sm text-slate-400">받은 알림이 없습니다.</li>
            )}
            {items.slice(0, 30).map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  onClick={() => handleSelect(n)}
                  className={`flex w-full flex-col items-start gap-1 border-b border-slate-50 px-4 py-3 text-left hover:bg-slate-50 ${
                    n.isRead ? "" : "bg-blue-50/60"
                  }`}
                >
                  <span className="text-sm text-slate-800">{n.message}</span>
                  <span className="text-xs text-slate-400">{relativeTime(n.createdAt)}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
