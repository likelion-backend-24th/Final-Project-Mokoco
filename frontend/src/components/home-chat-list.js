"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ChatCircle, ArrowRight, ArrowsClockwise } from "@phosphor-icons/react";

function formatTime(value) {
  if (!value) return "";
  const date = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] ?? 0, value[4] ?? 0, value[5] ?? 0)
    : new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("ko-KR", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

export default function HomeChatList({ isAuthenticated }) {
  const [page, setPage] = useState(0);
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState(null);
  const query = `${page}-${revision}`;
  const loading = isAuthenticated && result?.query !== query;
  const rooms = result?.query === query ? result.rooms : [];
  const error = result?.query === query ? result.error : null;

  useEffect(() => {
    if (!isAuthenticated) return;
    const controller = new AbortController();
    fetch(`/api/chat-rooms?page=${page}&size=5`, { cache: "no-store", signal: controller.signal })
      .then(async response => {
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || "채팅방 목록을 불러오지 못했습니다.");
        if (!Array.isArray(data)) throw new Error("채팅방 응답을 확인하지 못했습니다.");
        if (!controller.signal.aborted) setResult({ query, rooms: data, error: null });
      })
      .catch(failure => {
        if (!controller.signal.aborted) setResult({ query, rooms: [], error: failure.message });
      });
    return () => controller.abort();
  }, [isAuthenticated, page, query]);

  return <section id="my-chats" className="reference-card" aria-labelledby="my-chats-title">
    <div className="reference-card-heading">
      <h2 id="my-chats-title" className="flex items-center gap-2"><ChatCircle size={22} weight="duotone" />내 채팅</h2>
      {isAuthenticated && <button type="button" aria-label="채팅방 목록 새로고침" disabled={loading}
        onClick={() => { setPage(0); setRevision(value => value + 1); }} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-40">
        <ArrowsClockwise size={18} />
      </button>}
    </div>
    {!isAuthenticated ? <div className="px-5 pb-6 text-center">
      <p className="mb-4 text-sm text-slate-500">로그인하고 이웃과의 대화를 이어가세요.</p>
      <Link href="/login" className="wide-outline-button">로그인하기</Link>
    </div> : loading ? <p role="status" className="p-6 text-center text-sm text-slate-500">채팅방을 불러오는 중입니다.</p>
      : error ? <div className="px-5 pb-5"><p role="alert" className="mb-3 text-sm text-red-600">{error}</p>
        <button type="button" className="wide-outline-button" onClick={() => setRevision(value => value + 1)}>다시 시도</button>
      </div> : <>
        {!rooms.length ? <p role="status" className="px-5 py-8 text-center text-sm text-slate-500">{page === 0 ? "아직 개설된 채팅방이 없습니다." : "더 이상 채팅방이 없습니다."}</p>
          : <ul className="divide-y divide-slate-100 px-5">
            {rooms.map(room => <li key={room.chatRoomId}>
              <Link href={`/chat-rooms/${room.chatRoomId}`} className="group flex items-center gap-3 rounded-xl py-4 focus-visible:outline-2 focus-visible:outline-blue-500">
                <div className="min-w-0 flex-1">
                  <h3 className="truncate text-sm font-bold text-slate-800 group-hover:text-blue-600">{room.postTitle || "수리 요청 채팅"}</h3>
                  <p className="mt-1 truncate text-sm text-slate-500">{room.lastMessage || "아직 메시지가 없습니다. 첫 인사를 건네보세요."}</p>
                  <time className="mt-2 block text-xs text-slate-400">{formatTime(room.lastMessageAt || room.createdAt)}</time>
                </div>
                <ArrowRight size={18} className="shrink-0 text-slate-400 group-hover:text-blue-600" />
              </Link>
            </li>)}
          </ul>}
        {(page > 0 || rooms.length === 5) && <nav aria-label="채팅방 목록 페이지" className="flex items-center justify-between border-t border-slate-100 px-5 py-3 text-sm">
          <button type="button" disabled={page === 0} onClick={() => setPage(value => value - 1)} className="rounded-lg px-3 py-2 text-blue-600 disabled:text-slate-300">이전</button>
          <span className="text-slate-500">{page + 1} 페이지</span>
          <button type="button" disabled={rooms.length < 5} onClick={() => setPage(value => value + 1)} className="rounded-lg px-3 py-2 text-blue-600 disabled:text-slate-300">다음</button>
        </nav>}
      </>}
  </section>;
}
