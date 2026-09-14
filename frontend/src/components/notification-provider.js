"use client";

import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { useNotificationStore } from "@/store/notificationStore";

function chatWsUrl() {
  if (process.env.NEXT_PUBLIC_CHAT_WS_URL) return process.env.NEXT_PUBLIC_CHAT_WS_URL;
  return window.location.protocol === "https:"
    ? `wss://${window.location.host}/ws/chat`
    : "ws://localhost:8082/ws/chat";
}

/**
 * 앱 전역 알림 구독기.
 *
 * 로그인 여부는 반드시 서버 응답(/api/notifications 의 200/401)으로 판단한다.
 * access_token 쿠키는 /api/auth/signin 에서 httpOnly 로 발급되기 때문에
 * document.cookie 로는 절대 읽을 수 없고, 예전처럼 이걸 기준으로 로그인 여부를
 * 판단하면 일반 이메일/비밀번호 로그인 사용자는 항상 false 로 판정되어
 * 알림이 영영 뜨지 않는다 (과거 버그: hasSession()이 document.cookie 의
 * access_token 을 정규식으로 찾던 방식).
 *
 * 로그인 상태가 확인되면 초기 알림 목록을 불러오고, 채팅용 STOMP 브로커의
 * 개인 큐(/user/queue/notifications)를 구독해 실시간으로 새 알림을 스토어에
 * 넣는다. UI 는 렌더링하지 않는다.
 */
export default function NotificationProvider() {
  const { setAll, prepend, setConnected, reset } = useNotificationStore();
  const clientRef = useRef(null);

  useEffect(() => {
    let cancelled = false;

    async function connectIfLoggedIn() {
      // 이미 연결을 시도했거나 연결돼 있으면 중복으로 다시 붙지 않음
      if (clientRef.current) return;

      let data;
      try {
        const res = await fetch("/api/notifications", { cache: "no-store" });
        if (!res.ok) {
          // 401 등 → 로그인되어 있지 않음
          if (!cancelled) reset();
          return;
        }
        data = await res.json();
      } catch {
        return;
      }
      if (cancelled || !Array.isArray(data)) return;
      setAll(data);

      const url = chatWsUrl();
      if (!/^wss?:\/\//.test(url)) return;

      const client = new Client({
        brokerURL: url,
        reconnectDelay: 5000,
        connectionTimeout: 10000,
        heartbeatIncoming: 0,
        heartbeatOutgoing: 10000,
        beforeConnect: async () => {
          try {
            const sessionRes = await fetch("/api/chat/session", { cache: "no-store" });
            const session = await sessionRes.json();
            if (!sessionRes.ok) throw new Error(session.error);
            client.connectHeaders = { Authorization: `Bearer ${session.token}` };
          } catch {
            void client.deactivate();
          }
        },
        onConnect: () => {
          if (cancelled) return;
          setConnected(true);
          client.subscribe("/user/queue/notifications", (frame) => {
            try {
              prepend(JSON.parse(frame.body));
            } catch {
              /* ignore malformed frame */
            }
          });
        },
        onWebSocketClose: () => {
          setConnected(false);
          clientRef.current = null; // 다음 focus 시 재접속을 다시 시도할 수 있게 초기화
        },
        onStompError: () => {
          void client.deactivate();
        },
      });

      clientRef.current = client;
      client.activate();
    }

    connectIfLoggedIn();

    // 다른 탭에서 로그인했거나, 세션이 새로 생긴 뒤 이 탭으로 돌아왔을 때 재시도
    const onFocus = () => connectIfLoggedIn();
    window.addEventListener("focus", onFocus);

    return () => {
      cancelled = true;
      window.removeEventListener("focus", onFocus);
      setConnected(false);
      if (clientRef.current) {
        void clientRef.current.deactivate();
        clientRef.current = null;
      }
    };
  }, [setAll, prepend, setConnected, reset]);

  return null;
}
