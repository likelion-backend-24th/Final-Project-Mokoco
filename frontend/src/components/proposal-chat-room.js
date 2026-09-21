"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ChatCircle } from "@phosphor-icons/react";

export default function ProposalChatRoom({ proposalId }) {
  const [room, setRoom] = useState(null);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");
  const router = useRouter();
  const opening = useRef(false);
  const endpoint = `/api/chat-rooms/proposals/${proposalId}`;

  useEffect(() => {
    if (!proposalId) return;
    const controller = new AbortController();
    fetch(endpoint, { signal: controller.signal, cache: "no-store" })
      .then(async (response) => {
        const data = await response.json();
        if (response.ok) { setRoom(data); setStatus("ready"); }
        else if (response.status === 404 && data.code === "CHAT_ROOM_NOT_FOUND") setStatus("missing");
        else { setError(data.error || "채팅방을 확인하지 못했습니다."); setStatus("error"); }
      })
      .catch(() => {
        if (!controller.signal.aborted) { setError("채팅방을 확인하지 못했습니다."); setStatus("error"); }
      });
    return () => controller.abort();
  }, [endpoint, proposalId]);

  async function openRoom() {
    if (room) { router.push(`/chat-rooms/${room.chatRoomId}`); return; }
    if (opening.current) return;
    opening.current = true;
    setStatus("loading");
    setError("");
    try {
      const response = await fetch(endpoint, { method: "POST" });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "채팅방 요청에 실패했습니다.");
      setRoom(data);
      router.push(`/chat-rooms/${data.chatRoomId}`);
      setStatus("ready");
    } catch (failure) { setError(failure.message); setStatus("error"); }
    finally { opening.current = false; }
  }

  if (!proposalId) return <p className="mt-4 text-sm text-slate-500">견적 정보를 불러오지 못했습니다. 페이지를 새로고침해주세요.</p>;

  return (
    <div className="mt-4 border-t border-emerald-100 pt-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-600">
          {room ? "이 견적의 채팅방이 개설되었습니다." : "채택 전에도 이 견적에 대해 1:1로 상담할 수 있습니다."}
        </p>
        <button type="button" onClick={openRoom} disabled={status === "loading"}
          className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50">
          <ChatCircle size={18} />
          {status === "loading" ? "확인 중..." : room ? "채팅방 열기" : status === "error" ? "다시 시도" : "견적 상담하기"}
        </button>
      </div>
      {error && <p role="alert" className="mt-2 text-sm text-red-600">{error}</p>}

    </div>
  );
}
