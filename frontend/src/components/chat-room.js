"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Client } from "@stomp/stompjs";

export default function ChatRoom({ roomId }) {
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [status, setStatus] = useState("연결 중");
  const [error, setError] = useState("");
  const [userId, setUserId] = useState(null);
  const [more, setMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const clientRef = useRef(null);
  const bottom = useRef(null);

  function merge(rows) {
    setMessages(current => [...new Map([...current, ...rows].map(row => [row.messageId, row])).values()].sort((a, b) => a.messageId - b.messageId));
  }
  async function history(before) {
    const response = await fetch(`/api/chat-rooms/${roomId}/messages${before ? `?before=${before}` : ""}`, { cache: "no-store" });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error);
    merge(data);
    setMore(data.length === 50);
  }

  useEffect(() => {
    let active = true;
    const url = process.env.NEXT_PUBLIC_CHAT_WS_URL || (window.location.protocol === "https:" ? `wss://${window.location.host}/ws/chat` : "ws://localhost:8082/ws/chat");
    const client = new Client({
      brokerURL: url, reconnectDelay: 5000, connectionTimeout: 10000,
      heartbeatIncoming: 0, heartbeatOutgoing: 10000,
      beforeConnect: async () => {
        try {
          if (!/^wss?:\/\//.test(url)) throw new Error("올바른 채팅 연결 주소가 필요합니다.");
          const response = await fetch("/api/chat/session", { cache: "no-store" });
          const session = await response.json();
          if (!response.ok) throw new Error(session.error);
          if (!active) return;
          setUserId(session.userId);
          client.connectHeaders = { Authorization: `Bearer ${session.token}` };
        } catch (failure) {
          if (active) { setError(failure.message); setStatus("연결 실패"); }
          void client.deactivate();
        }
      },
      onConnect: () => {
        if (!active) return;
        setStatus("연결됨"); setError("");
        client.subscribe(`/topic/chat/${roomId}`, frame => {
          if (active) merge([JSON.parse(frame.body)]);
        });
        // Subscribe first, then merge persisted history to avoid a gap or duplicate rows.
        fetch(`/api/chat-rooms/${roomId}/messages`, { cache: "no-store" })
          .then(async response => { const data = await response.json(); if (!response.ok) throw new Error(data.error); return data; })
          .then(data => { if (active) { merge(data); setMore(data.length === 50); } })
          .catch(failure => { if (active) setError(failure.message); });
      },
      onWebSocketClose: () => { if (active) setStatus("재연결 중"); },
      onWebSocketError: () => { if (active) setError("채팅 연결에 실패했습니다. 서버와 연결 주소를 확인해주세요."); },
      onStompError: () => { if (active) { setError("채팅 권한 또는 전송 내용을 확인해주세요."); setStatus("연결 실패"); void client.deactivate(); } },
    });
    clientRef.current = client;
    client.activate();
    return () => { active = false; clientRef.current = null; void client.deactivate(); };
  }, [roomId]);

  useEffect(() => { bottom.current?.scrollIntoView({ behavior: "smooth" }); }, [messages.length]);

  function send(event) {
    event.preventDefault();
    if (!text.trim() || !clientRef.current?.connected) return;
    try {
      clientRef.current.publish({ destination: `/app/chat/${roomId}`, body: JSON.stringify({ content: text.trim(), type: "TEXT" }) });
      setText("");
    } catch { setError("전송하지 못했습니다. 다시 시도해주세요."); }
  }

  return <main className="mx-auto flex h-dvh max-w-3xl flex-col bg-slate-50 p-4">
    <header className="flex items-center justify-between border-b p-3">
      <div><Link href="/posts" className="text-sm text-blue-600">수리 요청 목록</Link><h1 className="text-xl font-bold">채팅방 #{roomId}</h1></div>
      <span role="status" className="text-sm text-slate-500">{status}</span>
    </header>
    {error && <p role="alert" className="p-3 text-sm text-red-600">{error}</p>}
    <div className="flex-1 overflow-y-auto p-3" role="log" aria-label="채팅 메시지" aria-live="polite">
      {more && <button disabled={loading} className="mb-4 text-sm text-blue-600" onClick={async () => {
        setLoading(true); try { await history(messages[0]?.messageId); } catch (failure) { setError(failure.message); } finally { setLoading(false); }
      }}>{loading ? "불러오는 중…" : "이전 메시지 더 보기"}</button>}
      {!messages.length && status === "연결됨" && <p className="py-10 text-center text-slate-400">첫 메시지를 보내보세요.</p>}
      {messages.map(message => <div key={message.messageId} className={`mb-3 flex ${message.senderId === userId ? "justify-end" : "justify-start"}`}>
        <div className={`max-w-[85%] whitespace-pre-wrap break-words rounded-2xl px-4 py-3 ${message.senderId === userId ? "bg-blue-600 text-white" : "bg-white text-slate-800"}`}>
          <p>{message.content}</p><time className="mt-1 block text-right text-xs opacity-60">{Array.isArray(message.createdAt) ? message.createdAt.slice(3, 5).map(n => String(n).padStart(2, "0")).join(":") : new Date(message.createdAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}</time>
        </div>
      </div>)}<div ref={bottom} />
    </div>
    <form onSubmit={send} className="flex gap-2 border-t pt-3">
      <input aria-label="메시지" maxLength={2000} value={text} onChange={event => setText(event.target.value)} placeholder="메시지를 입력하세요" className="min-w-0 flex-1 rounded-xl border bg-white p-3" />
      <button disabled={status !== "연결됨" || !text.trim()} className="rounded-xl bg-blue-600 px-5 text-white disabled:opacity-40">전송</button>
    </form>
  </main>;
}
