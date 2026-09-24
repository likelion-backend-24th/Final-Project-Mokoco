"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Client } from "@stomp/stompjs";
import ChatAttachment from "@/components/chat-attachment";
import ContractModal from "@/components/contract-modal";
import { ArrowLeft, ArrowUp, ChatCircleDots, Gear, ShieldCheck, User, Wrench } from "@phosphor-icons/react";
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

// 채팅에서 계약서 화면으로 넘어갈 때 지금 뭘 해야 하는지 바로 보이도록 배너 문구를 거래 상태별로 다르게 보여준다.
const dealBannerLabel = {
  MATCHED: "계약서 작성하고 결제하기 →",
  PRODUCT_SENT: "수리 진행 상황 보기 →",
  REPAIRING: "수리 진행 상황 보기 →",
  REPAIR_DONE: "완료 확인하기 →",
  COMPLETED: "계약서 · 후기 보기 →",
};
// 떠 있는 작은 알약 버튼에는 위 문구가 너무 기니, 짧은 버전만 보여주고 전체 문구는 title/aria-label로 남긴다.
const dealPillLabel = {
  MATCHED: "계약서 작성",
  PRODUCT_SENT: "진행 상황",
  REPAIRING: "진행 상황",
  REPAIR_DONE: "완료 확인",
  COMPLETED: "계약서·후기",
};

export default function ChatRoom({ roomId, embedded = false, onBack }) {
  const router = useRouter();
  const goBack = onBack ?? (() => router.back());
  const [theme, selectTheme] = useChatTheme();
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [status, setStatus] = useState("연결 중");
  const [error, setError] = useState("");
  const [userId, setUserId] = useState(null);
  const [contractOpen, setContractOpen] = useState(false);
  const [counterpart, setCounterpart] = useState(null);
  const [detail, setDetail] = useState(null);
  const nickname = counterpart?.roomId === roomId ? counterpart.nickname : "";
  const [more, setMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [themeMenuOpen, setThemeMenuOpen] = useState(false);
  const clientRef = useRef(null);
  const bottom = useRef(null);
  const themeMenuRef = useRef(null);
  const pressTimer = useRef(null);
  const touchDeleteFired = useRef(false);

  useEffect(() => () => { if (pressTimer.current) clearTimeout(pressTimer.current); }, []);

  useEffect(() => {
    if (!themeMenuOpen) return;
    const onClick = event => {
      if (themeMenuRef.current && !themeMenuRef.current.contains(event.target)) setThemeMenuOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, [themeMenuOpen]);

  function merge(rows) {
    setMessages(current => {
      const map = new Map(current.map(row => [row.messageId, row]));
      for (const row of rows) if (!map.get(row.messageId)?.deleted) map.set(row.messageId, row);
      return [...map.values()].sort((a, b) => a.messageId - b.messageId);
    });
  }
  async function deleteMessage(messageId) {
    if (!window.confirm("메시지를 삭제하시겠습니까? 상대방에게도 삭제된 메시지로 표시됩니다.")) return;
    setError("");
    try {
      const response = await fetch(`/api/chat-rooms/${roomId}/messages/${messageId}`, { method: "DELETE" });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error);
      merge([data]);
    } catch (failure) { setError(failure.message); }
  }
  // 삭제 버튼을 따로 안 두고, 데스크톱은 우클릭, 모바일은 길게 눌렀을 때만 삭제를 물어본다.
  function startLongPress(messageId) {
    touchDeleteFired.current = false;
    pressTimer.current = setTimeout(() => {
      touchDeleteFired.current = true;
      deleteMessage(messageId);
    }, 550);
  }
  function cancelLongPress() {
    if (pressTimer.current) { clearTimeout(pressTimer.current); pressTimer.current = null; }
  }
  function handleContextMenu(event, messageId) {
    event.preventDefault();
    // 모바일에서 롱프레스가 이미 삭제를 띄웠다면(브라우저가 그 제스처로 contextmenu도 같이
    // 쏘는 경우가 있음) 확인창이 두 번 뜨지 않게 여기서는 건너뛴다.
    if (touchDeleteFired.current) { touchDeleteFired.current = false; return; }
    deleteMessage(messageId);
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

  useEffect(() => { bottom.current?.scrollIntoView({ behavior: "smooth", block: "nearest" }); }, [messages.length]);

  useEffect(() => {
    let active = true;
    function loadDetail() {
      fetch(`/api/chat-rooms/${roomId}/detail`, { cache: "no-store" })
        .then(async response => { const data = await response.json(); if (!response.ok) throw new Error(data.error); return data; })
        .then(data => { if (active) setDetail({ roomId, ...data }); })
        .catch(() => {});
    }
    loadDetail();
    window.addEventListener("focus", loadDetail);
    return () => { active = false; window.removeEventListener("focus", loadDetail); };
  }, [roomId]);

  function send(event) {
    event.preventDefault();
    if (!text.trim() || !clientRef.current?.connected) return;
    try {
      clientRef.current.publish({ destination: `/app/chat/${roomId}`, body: JSON.stringify({ content: text.trim(), type: "TEXT" }) });
      setText("");
    } catch { setError("전송하지 못했습니다. 다시 시도해주세요."); }
  }

  const hasDealFab = detail?.roomId === roomId && Boolean(detail.fixDealId);

  const shell = <main className="conversation-shell" data-theme={theme} onClick={event => event.stopPropagation()}>
    <header className="conversation-header">
      <button type="button" onClick={goBack} className="chat-icon-button" aria-label="뒤로가기"><ArrowLeft size={22} /></button>
      <div className="conversation-mark"><Wrench size={24} weight="duotone" /></div>
      <div className="conversation-heading">
        <h1>{nickname || "수리 상담"}</h1>
        {detail?.roomId === roomId && detail.postId
          ? <Link href={`/posts/${detail.postId}`} className="conversation-post-link">{detail.postTitle || "글 보러 가기"}</Link>
          : <span>동네수리 · 1:1 대화</span>}
      </div>
      {status !== "연결됨" && <span role="status" className="connection-status">{status}</span>}
      <div className="theme-menu-anchor" ref={themeMenuRef}>
        <button type="button" onClick={() => setThemeMenuOpen(open => !open)} className="chat-icon-button" aria-label="채팅 테마 설정" aria-expanded={themeMenuOpen}><Gear size={18} /></button>
        {themeMenuOpen && (
          <div className="theme-menu-popover">
            <label htmlFor="chat-theme">채팅 테마</label>
            <select id="chat-theme" value={theme} onChange={event => selectTheme(event.target.value)}>
              {chatThemes.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
            </select>
          </div>
        )}
      </div>
    </header>

    {error && <p role="alert" className="conversation-error">{error}</p>}
    <div className="conversation-messages" role="log" aria-label="채팅 메시지" aria-live="polite">
      {more && <button className="history-button" disabled={loading} onClick={async () => {
        setLoading(true); try { await history(messages[0]?.messageId); } catch (failure) { setError(failure.message); } finally { setLoading(false); }
      }}>{loading ? "불러오는 중…" : "이전 대화 보기"}</button>}
      {!messages.length && <div className="conversation-empty"><ChatCircleDots size={44} weight="duotone" /><h2>{status === "연결됨" ? "반가운 첫 인사를 건네보세요" : "대화를 준비하고 있어요"}</h2><p>수리가 필요한 부분과 궁금한 점을 나눠보세요.</p></div>}
      {messages.map((message, index) => {
        const mine = message.senderId === userId;
        const date = messageDate(message.createdAt);
        const prev = messages[index - 1];
        const newDay = index === 0 || dayLabel(prev.createdAt) !== dayLabel(message.createdAt);
        // 같은 사람이 연달아 보낸 메시지는 닉네임/아바타를 맨 위 하나에만 보여준다.
        const isGroupStart = newDay || !prev || prev.senderId !== message.senderId;
        return <div key={message.messageId}>
          {newDay && <div className="conversation-date"><span>{dayLabel(message.createdAt)}</span></div>}
          <article className={`conversation-row ${mine ? "is-mine" : ""} ${isGroupStart ? "is-group-start" : ""}`}>
            {!mine && (
              <div className="conversation-avatar-slot">
                {isGroupStart && <div className="conversation-avatar" aria-hidden="true"><User size={17} weight="duotone" /></div>}
              </div>
            )}
            <div className="conversation-message">
              <div className="message-bubble-row">
                <div
                  className={`message-bubble ${message.attachmentUrl ? "has-media" : ""} ${message.deleted ? "is-deleted" : ""}`}
                  onContextMenu={mine && !message.deleted ? event => handleContextMenu(event, message.messageId) : undefined}
                  onTouchStart={mine && !message.deleted ? () => startLongPress(message.messageId) : undefined}
                  onTouchEnd={mine && !message.deleted ? cancelLongPress : undefined}
                  onTouchMove={mine && !message.deleted ? cancelLongPress : undefined}
                  onTouchCancel={mine && !message.deleted ? cancelLongPress : undefined}
                >
                  {message.attachmentUrl ? message.type === "VIDEO"
                    ? <video src={message.attachmentUrl} controls preload="metadata" />
                    : <a href={message.attachmentUrl} target="_blank" rel="noreferrer">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={message.attachmentUrl} alt="첨부 이미지" loading="lazy" />
                    </a>
                    : <p>{message.content}</p>}
                </div>
                <div className="message-side">
                  <time>{date?.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}</time>
                </div>
              </div>
            </div>
          </article>
        </div>;
      })}<div ref={bottom} />
    </div>
    {hasDealFab && (
      <div className="deal-banner-wrap">
        <button
          type="button"
          onClick={() => setContractOpen(true)}
          className="deal-fab"
          title={dealBannerLabel[detail.dealStatus] || "수리 계약서 작성하기"}
          aria-label={dealBannerLabel[detail.dealStatus] || "수리 계약서 작성하기"}
        >
          <ShieldCheck size={15} weight="fill" />
          <span>{dealPillLabel[detail.dealStatus] || "계약서"}</span>
        </button>
      </div>
    )}

    {contractOpen && detail?.fixDealId && (
      <ContractModal roomId={roomId} onClose={() => setContractOpen(false)} />
    )}

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
    </footer>
  </main>;

  if (embedded) return shell;
  return <div className="conversation-overlay" onClick={() => router.back()}>{shell}</div>;
}