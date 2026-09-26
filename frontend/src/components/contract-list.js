"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { CaretDown } from "@phosphor-icons/react";
import "./repair-contract.css";
import { ContractDocument, dealLabels } from "./repair-contract";

function ContractSummary({ roomId }) {
  const [overview, setOverview] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/chat-rooms/${roomId}/contract`, { cache: "no-store", signal: controller.signal })
      .then(async response => {
        const data = await response.json();
        if (!response.ok) throw new Error(data.error);
        setOverview(data);
      })
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message ?? "계약서를 불러오지 못했습니다."); });
    return () => controller.abort();
  }, [roomId]);

  if (error) return <p role="alert" className="text-sm text-red-600">{error}</p>;
  if (!overview) return <p className="text-sm text-slate-400">불러오는 중...</p>;
  const latest = overview.versions[0];
  return <div className="contract-page contract-embedded contract-summary">
    <p className="text-sm text-slate-600">거래 상태: <strong>{dealLabels[overview.dealStatus] || overview.dealStatus}</strong></p>
    {latest
      ? <ContractDocument selected={latest} overview={overview} roomId={roomId} />
      : <p className="text-sm text-slate-500">아직 작성된 계약서가 없어요. 채팅에서 합의한 조건으로 초안을 작성해주세요.</p>}
    <p><Link href={`/chat-rooms/${roomId}`} className="text-sm text-blue-600">채팅에서 열어 서명·진행하기 →</Link></p>
  </div>;
}

export default function ContractList() {
  const [rooms, setRooms] = useState([]);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");
  const [openRooms, setOpenRooms] = useState({});

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/chat-rooms?page=0&size=50", { cache: "no-store", signal: controller.signal })
      .then(async response => {
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || "계약서 목록을 불러오지 못했습니다.");
        setRooms((Array.isArray(data) ? data : []).filter(room => room.fixDealId));
        setStatus("ready");
      })
      .catch(failure => {
        if (controller.signal.aborted) return;
        setError(failure.message);
        setStatus("error");
      });
    return () => controller.abort();
  }, []);

  return <section className="mt-8">
    <h2 className="text-lg font-semibold text-slate-800">내 계약서</h2>
    <p className="mt-1 text-sm text-slate-500">채택된 견적으로 진행 중이거나 끝난 계약서를 눌러서 바로 확인해보세요.</p>
    {status === "loading" && <p className="mt-4 text-sm text-slate-400">불러오는 중...</p>}
    {status === "error" && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>}
    {status === "ready" && rooms.length === 0 && <p className="mt-4 rounded-xl border border-slate-200 bg-white px-4 py-6 text-center text-sm text-slate-400">아직 계약서가 없어요.</p>}
    {status === "ready" && rooms.length > 0 && <ul className="mt-4 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
      {rooms.map(room => <li key={room.chatRoomId}>
        <details className="group" onToggle={event => { const isOpen = event.currentTarget.open; setOpenRooms(current => ({ ...current, [room.chatRoomId]: isOpen })); }}>
          <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-4 py-4 [&::-webkit-details-marker]:hidden">
            <div>
              <p className="text-sm font-semibold text-slate-800">{room.postTitle || `수리 요청 #${room.postId}`}</p>
              <p className="mt-0.5 text-xs text-slate-500">수리 요청 #{room.postId} · 채팅방 #{room.chatRoomId}</p>
            </div>
            <CaretDown size={18} className="shrink-0 text-slate-400 transition-transform group-open:rotate-180" />
          </summary>
          <div className="border-t border-slate-100 px-4 py-4">
            {openRooms[room.chatRoomId] && <ContractSummary roomId={room.chatRoomId} />}
          </div>
        </details>
      </li>)}
    </ul>}
  </section>;
}
