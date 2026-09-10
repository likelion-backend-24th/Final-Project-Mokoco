"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Client } from "@stomp/stompjs";
import ChatAttachment from "@/components/chat-attachment";
import { Trash, ArrowLeft, ArrowUp, ChatCircleDots, ShieldCheck, User, Wrench } from "@phosphor-icons/react";
import "./chat-room.css";
import { chatThemes, useChatTheme } from "@/components/chat-theme";

function messageDate(value) {
  if (!value) return null;
  const date = Array.isArray(value) ? new Date(value[0], value[1] - 1, value[2], value[3] || 0, value[4] || 0) : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function dayLabel(value) {
  return messageDate(value)?.toLocaleDateString("ko-KR", { month: "long", day: "numeric", weekday: "long" }) || "";
}

export default function ChatRoom({ roomId }) {
  const [theme, selectTheme] = useChatTheme();
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [status, setStatus] = useState("연결 중");
  const [error, setError] = useState("");
  const [userId, setUserId] = useState(null);
  const [counterpart, setCounterpart] = useState(null);
  const nickname = counterpart?.roomId === roomId ? counterpart.nickname : "";
  const [more, setMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [deleting, setDeleting] = useState(null);
  const clientRef = useRef(null);
  const bottom = useRef(null);

  function merge(rows) {
    setMessages(current => {
      const map = new Map(current.map(row => [row.messageId, row]));
      for (const row of rows) if (!map.get(row.messageId)?.deleted) map.set(row.messageId, row);
      return [...map.values()].sort((a, b) => a.messageId - b.messageId);
    });
  }
  async function deleteMessage(messageId) {
    if (!window.confirm("메시지를 삭제하시겠습니까? 상대방에게도 삭제된 메시지로 표시됩니다.")) return;
    setDeleting(messageId); setError("");
    try {
      const response = await fetch(`/api/chat-rooms/${roomId}/messages/${messageId}`, { method: "DELETE" });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error);
      merge([data]);
    } catch (failure) { setError(failure.message); }
    finally { setDeleting(null); }
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
        fetch(`/api/chat-rooms/${roomId}/counterpart`, { cache: "no-store" })
          .then(async response => { const data = await response.json(); if (!response.ok) throw new Error(data.error); return data; })
          .then(data => { if (active) setCounterpart({ roomId, nickname: data.nickname?.trim() || "이웃" }); })
          .catch(failure => { if (active) setError(failure.message); });
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

  return <main className="conversation-shell" data-theme={theme}>
    <header className="conversation-header">
      <Link href="/#my-chats" className="chat-icon-button" aria-label="내 채팅 목록으로"><ArrowLeft size={22} /></Link>
      <div className="conversation-mark"><Wrench size={24} weight="duotone" /></div>
      <div className="conversation-heading"><span>동네수리 · 1:1 대화</span><h1>{nickname || "수리 상담"} <small>#{roomId}</small></h1></div>
      <span role="status" className={`connection-status ${status === "연결됨" ? "is-connected" : ""}`}>{status}</span>
    </header>
    <div className="chat-theme-bar">
      <label htmlFor="chat-theme">채팅 테마</label>
      <select id="chat-theme" value={theme} onChange={event => selectTheme(event.target.value)}>
        {chatThemes.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
      </select>
      <span>내 화면에만 적용</span>
    </div>

    <div className="conversation-banner"><ShieldCheck size={17} /><span>수리 범위와 일정을 이곳에서 함께 확인하세요.</span></div>
    {error && <p role="alert" className="conversation-error">{error}</p>}
    <div className="conversation-messages" role="log" aria-label="채팅 메시지" aria-live="polite">
      {more && <button className="history-button" disabled={loading} onClick={async () => {
        setLoading(true); try { await history(messages[0]?.messageId); } catch (failure) { setError(failure.message); } finally { setLoading(false); }
      }}>{loading ? "불러오는 중…" : "이전 대화 보기"}</button>}
      {!messages.length && <div className="conversation-empty"><ChatCircleDots size={44} weight="duotone" /><h2>{status === "연결됨" ? "반가운 첫 인사를 건네보세요" : "대화를 준비하고 있어요"}</h2><p>수리가 필요한 부분과 궁금한 점을 나눠보세요.</p></div>}
      {messages.map((message, index) => {
        const mine = message.senderId === userId;
        const date = messageDate(message.createdAt);
        const newDay = index === 0 || dayLabel(messages[index - 1].createdAt) !== dayLabel(message.createdAt);
        return <div key={message.messageId}>
          {newDay && <div className="conversation-date"><span>{dayLabel(message.createdAt)}</span></div>}
          <article className={`conversation-row ${mine ? "is-mine" : ""}`}>
            {!mine && <div className="conversation-avatar" aria-hidden="true"><User size={21} weight="duotone" /></div>}
            <div className="conversation-message">
              <div className="message-meta"><span>{mine ? "나" : nickname || "이웃"}</span><time>{date?.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}</time>
                {mine && !message.deleted && <button type="button" aria-label="메시지 삭제" title="메시지 삭제" disabled={deleting !== null} onClick={() => deleteMessage(message.messageId)} className="message-delete"><Trash size={14} /></button>}
              </div>
              <div className={`message-bubble ${message.attachmentUrl ? "has-media" : ""} ${message.deleted ? "is-deleted" : ""}`}>
                {message.attachmentUrl ? message.type === "VIDEO"
                  ? <video src={message.attachmentUrl} controls preload="metadata" />
                  : <a href={message.attachmentUrl} target="_blank" rel="noreferrer">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={message.attachmentUrl} alt="첨부 이미지" loading="lazy" />
                  </a>
                  : <p>{message.content}</p>}
              </div>
            </div>
          </article>
        </div>;
      })}<div ref={bottom} />
    </div>
    <Link href={`/chat-rooms/${roomId}/contract`} className="conversation-banner" style={{ fontWeight: 600 }}>수리 계약서 작성 · 서명 · 작업 진행 →</Link>

    <footer className="conversation-footer">
      <div className="conversation-composer">
        <ChatAttachment key={roomId} roomId={roomId} onSent={message => merge([message])} />
        <form onSubmit={send} className="message-form">
          <textarea rows={1} aria-label="메시지" maxLength={2000} value={text} onChange={event => setText(event.target.value)}
            onKeyDown={event => { if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) { event.preventDefault(); send(event); } }}
            placeholder="메시지를 입력하세요" />
          <button aria-label="메시지 전송" disabled={status !== "연결됨" || !text.trim()} className="message-send"><ArrowUp size={23} weight="bold" /></button>
        </form>
      </div>
      <p className="composer-hint">사진과 동영상으로 수리할 부분을 더 자세히 알려주세요.</p>
    </footer>
  </main>;
}
