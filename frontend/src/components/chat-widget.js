"use client";

import { useEffect, useMemo, useState } from "react";
import { ChatCircleDots, ArrowRight, X, Wrench } from "@phosphor-icons/react";
import ChatRoom from "@/components/chat-room";
import { useChatWidgetStore } from "@/store/chatWidgetStore";
import { useAuthStore } from "@/store/authStore";
import { useNotificationStore } from "@/store/notificationStore";
import "./chat-widget.css";

function formatTime(value) {
  if (!value) return "";
  const date = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] ?? 0, value[4] ?? 0, value[5] ?? 0)
    : new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("ko-KR", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

export default function ChatWidget() {
  const isAuthenticated = useAuthStore(state => Boolean(state.accessToken));
  const open = useChatWidgetStore(state => state.open);
  const activeRoomId = useChatWidgetStore(state => state.activeRoomId);
  const openList = useChatWidgetStore(state => state.openList);
  const backToListAction = useChatWidgetStore(state => state.backToList);
  const closeAction = useChatWidgetStore(state => state.close);
  const openRoom = useChatWidgetStore(state => state.openRoom);
  // items 자체(참조)만 구독하고, 파생값(Set 등)은 useMemo로 따로 계산한다 — 셀렉터가
  // 매번 새 객체를 반환하면 useSyncExternalStore가 "항상 바뀐 값"으로 보고 무한 리렌더
  // 루프에 빠진다(전역 마운트라 전 페이지가 그 여파로 멈춤).
  const notificationItems = useNotificationStore(state => state.items);
  const unreadChatCount = useMemo(
    () => notificationItems.filter(n => n.type === "CHAT_MESSAGE" && !n.isRead).length,
    [notificationItems]
  );
  // 목록에서 "어느 방에 새 메시지가 왔는지" 바로 보이도록 방 id 단위로 미읽음 여부를 묶어둔다.
  const unreadRoomIds = useMemo(() => new Set(
    notificationItems.filter(n => n.type === "CHAT_MESSAGE" && !n.isRead && n.chatRoomId != null).map(n => n.chatRoomId)
  ), [notificationItems]);
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // 채팅방을 열면(목록에서 선택하든, 다른 화면의 버튼으로 바로 들어오든) 그 방에 대한
  // 미읽음 채팅 알림은 새로 읽은 것으로 처리한다 — 별도 라우트로 안 흩어지게 이 한 곳에서만 정리.
  useEffect(() => {
    if (!activeRoomId) return;
    const { items, markRead } = useNotificationStore.getState();
    items
      .filter(n => n.type === "CHAT_MESSAGE" && n.chatRoomId === activeRoomId && !n.isRead)
      .forEach(n => {
        markRead(n.id);
        fetch(`/api/notifications/${n.id}/read`, { method: "PATCH" }).catch(() => {});
      });
  }, [activeRoomId]);

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
    openList();
    setLoading(true);
    setError("");
  }

  function backToList() {
    backToListAction();
    setLoading(true);
    setError("");
  }

  if (!isAuthenticated) return null;

  return <>
    {!open && (
      <button
        type="button"
        onClick={openWidget}
        className="chat-widget-fab"
        aria-label={`채팅 열기${unreadChatCount ? ` (안 읽은 메시지 ${unreadChatCount}개)` : ""}`}
      >
        <ChatCircleDots size={26} weight="fill" />
        {unreadChatCount > 0 && (
          <span className="chat-widget-fab-badge">{unreadChatCount > 99 ? "99+" : unreadChatCount}</span>
        )}
      </button>
    )}

    {open && (
      <div className="conversation-overlay chat-widget-overlay" onClick={closeAction}>
        {activeRoomId ? (
          <ChatRoom roomId={activeRoomId} embedded onBack={backToList} />
        ) : (
          <div className="conversation-shell chat-widget-list" onClick={event => event.stopPropagation()}>
            <header className="conversation-header">
              <div className="conversation-mark"><ChatCircleDots size={22} weight="fill" /></div>
              <div className="conversation-heading"><span>동네수리</span><h1>내 채팅</h1></div>
              <button type="button" onClick={closeAction} className="chat-icon-button" aria-label="닫기"><X size={20} /></button>
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
                  {rooms.map(room => {
                    const unread = unreadRoomIds.has(room.chatRoomId);
                    return (
                      <li key={room.chatRoomId}>
                        <button
                          type="button"
                          onClick={() => openRoom(room.chatRoomId)}
                          className={`chat-widget-room-row ${unread ? "is-unread" : ""}`}
                        >
                          <div className="chat-widget-room-icon"><Wrench size={18} weight="duotone" /></div>
                          <div className="chat-widget-room-text">
                            <h3>{room.counterpartNickname || "이웃"}{unread && <span className="chat-widget-unread-dot" aria-label="새 메시지" />}</h3>
                            {room.postTitle && <p className="chat-widget-room-post">{room.postTitle}</p>}
                            <p className="chat-widget-room-preview">{room.lastMessage || "아직 메시지가 없습니다. 첫 인사를 건네보세요."}</p>
                            <time>{formatTime(room.lastMessageAt || room.createdAt)}</time>
                          </div>
                          <ArrowRight size={16} weight="bold" />
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </div>
        )}
      </div>
    )}
  </>;
}
