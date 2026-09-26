"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowRight, Wrench } from "@phosphor-icons/react";
import { DEAL_STATUS_LABEL } from "@/lib/deal-status-label";

const TABS = [
  { key: "requester", label: "요청한 거래" },
  { key: "repairer", label: "도와준 거래" },
];

export default function ActiveDealsCard({ deals }) {
  const [tab, setTab] = useState("requester");
  const list = deals[tab] ?? [];

  return (
    <section className="reference-card activity-card">
      <div className="reference-card-heading">
        <h2>진행중인 거래</h2>
        <Link href="/profile">전체 보기 <ArrowRight size={14} /></Link>
      </div>
      <div className="active-deal-tabs">
        {TABS.map(({ key, label }) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key)}
            className={`active-deal-tab ${tab === key ? "active-deal-tab-selected" : ""}`}
          >
            {label} ({deals[key]?.length ?? 0})
          </button>
        ))}
      </div>
      {list.length === 0 ? (
        <>
          <p>아직 진행 중인 거래가 없어요. 주변 수리 요청을 둘러보고 제안해보세요.</p>
          <Link href="/posts" className="wide-outline-button">주변 요청 보기</Link>
        </>
      ) : (
        <ul className="active-deal-list">
          {list.map((deal) => (
            <li key={deal.fixDealId}>
              <Link href={`/posts/${deal.postId}`} className="active-deal-row">
                <div className="active-deal-icon" aria-hidden="true"><Wrench size={20} weight="duotone" /></div>
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold text-slate-800">{deal.postTitle}</p>
                  <p className="text-sm text-slate-500">{deal.counterpartNickname || deal.counterpartEmail}</p>
                </div>
                <span className={`status-badge shrink-0 ${deal.status === "MATCHED" ? "status-matched" : ""}`}>
                  {DEAL_STATUS_LABEL[deal.status] ?? deal.status}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
