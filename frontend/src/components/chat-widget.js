"use client";

import { useEffect, useState } from "react";
import { ChatCircleDots, ArrowRight, X } from "@phosphor-icons/react";
import ChatRoom from "@/components/chat-room";
import "./chat-widget.css";

function formatTime(value) {
  if (!value) return "";
  const date = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] ?? 0, value[4] ?? 0, value[5] ?? 0)
    : new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("ko-KR", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

export default function ChatWidget({ isAuthenticated }) {
  const [open, setOpen] = useState(false);
  const [activeRoomId, setActiveRoomId] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open || activeRoomId || !isAuthenticated) return;
    const controller = new AbortController();
    fetch("/api/chat-rooms?page=0&size=20", { cache: "no-store", signal: controller.signal })
      .then(async response => {
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || "채팅방 목록을 불러오지 못했습니다.");
        if (!controller.signal.aborted) setRooms(Array.isArray(data) ? data : []);
      })
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [open, activeRoomId, isAuthenticated]);

  function openWidget() {
    setOpen(true);
    setLoading(true);
    setError("");
  }

  function backToList() {
    setActiveRoomId(null);
    setLoading(true);
    setError("");
  }

  function close() {
    setOpen(false);
    setActiveRoomId(null);
  }

  if (!isAuthenticated) return null;

  return <>
    {!open && (
      <button type="button" onClick={openWidget} className="chat-widget-fab" aria-label="채팅 열기">
        <ChatCircleDots size={26} weight="fill" />
      </button>
    )}

    {open && (
      <div className="conversation-overlay" onClick={close}>
        {activeRoomId ? (
          <div onClick={event => event.stopPropagation()}>
            <ChatRoom roomId={activeRoomId} embedded onBack={backToList} />
          </div>
        ) : (
          <div className="conversation-shell chat-widget-list" onClick={event => event.stopPropagation()}>
            <header className="conversation-header">
              <div className="conversation-heading"><span>동네수리</span><h1>내 채팅</h1></div>
              <button type="button" onClick={close} className="chat-icon-button" aria-label="닫기"><X size={20} /></button>
            </header>
            <div className="conversation-messages chat-widget-room-list" role="log" aria-label="내 채팅방 목록">
              {loading ? (
                <p className="chat-widget-empty">채팅방을 불러오는 중입니다.</p>
              ) : error ? (
                <p role="alert" className="chat-widget-empty">{error}</p>
              ) : !rooms.length ? (
                <p className="chat-widget-empty">아직 개설된 채팅방이 없습니다.</p>
              ) : (
                <ul>
                  {rooms.map(room => (
                    <li key={room.chatRoomId}>
                      <button type="button" onClick={() => setActiveRoomId(room.chatRoomId)} className="chat-widget-room-row">
                        <div>
                          <h3>{room.postTitle || "수리 요청 채팅"}</h3>
                          <p>{room.lastMessage || "아직 메시지가 없습니다. 첫 인사를 건네보세요."}</p>
                          <time>{formatTime(room.lastMessageAt || room.createdAt)}</time>
                        </div>
                        <ArrowRight size={16} weight="bold" />
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </div>
    )}
  </>;
}
