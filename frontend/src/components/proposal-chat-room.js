"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ChatCircle } from "@phosphor-icons/react";

export default function ProposalChatRoom({ fixDealId, isRequester }) {
  const [room, setRoom] = useState(null);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");
  const router = useRouter();
  const endpoint = `/api/chat-rooms/fix-deals/${fixDealId}`;

  useEffect(() => {
    if (!fixDealId) return;
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
  }, [endpoint, fixDealId]);

  async function openRoom() {
    if (room) { router.push(`/chat-rooms/${room.chatRoomId}`); return; }
    setStatus("loading");
    setError("");
    try {
      // Check again before creating: another tab may already have opened the room.
      let response = await fetch(endpoint, { cache: "no-store" });
      let data = await response.json();
      if (response.status === 404 && data.code === "CHAT_ROOM_NOT_FOUND" && isRequester) {
        response = await fetch(endpoint, { method: "POST" });
        data = await response.json();
        if (data.code === "CHAT_ROOM_ALREADY_EXISTS") {
          response = await fetch(endpoint, { cache: "no-store" });
          data = await response.json();
        }
      }
      if (!response.ok) throw new Error(data.error || "채팅방 요청에 실패했습니다.");
      setRoom(data);
      router.push(`/chat-rooms/${data.chatRoomId}`);
      setStatus("ready");
    } catch (failure) { setError(failure.message); setStatus("error"); }
  }

  if (!fixDealId) return <p className="mt-4 text-sm text-slate-500">거래 정보를 불러오지 못했습니다. 페이지를 새로고침해주세요.</p>;

  return (
    <div className="mt-4 border-t border-emerald-100 pt-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-600">
          {room ? "이 제안의 채팅방이 개설되었습니다." : isRequester ? "채택한 이웃과의 채팅방을 개설할 수 있습니다." : "요청자가 채팅방을 개설하면 확인할 수 있습니다."}
        </p>
        <button type="button" onClick={openRoom} disabled={status === "loading"}
          className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50">
          <ChatCircle size={18} />
          {status === "loading" ? "확인 중..." : room ? "채팅방 열기" : status === "error" ? "다시 시도" : isRequester ? "채팅방 개설하기" : "개설 여부 확인"}
        </button>
      </div>
      {error && <p role="alert" className="mt-2 text-sm text-red-600">{error}</p>}

    </div>
  );
}
