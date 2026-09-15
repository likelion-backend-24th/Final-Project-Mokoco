"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Star } from "@phosphor-icons/react";

const STATUS_LABEL = {
  MATCHED: "매칭 완료",
  PRODUCT_SENT: "제품 전달 완료",
  REPAIRING: "수리 진행중",
  REPAIR_DONE: "수리완료 신청됨",
  COMPLETED: "거래 완료",
  CANCELED: "거래 취소됨",
};

const TABS = [
  { key: "requester", label: "의뢰자로 참여한 거래" },
  { key: "repairer", label: "수리자로 참여한 거래" },
  { key: "written-reviews", label: "내가 작성한 후기" },
  { key: "received-reviews", label: "내가 받은 후기" },
];

function StarRow({ rating }) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((value) => (
        <Star
          key={value}
          size={13}
          weight={value <= rating ? "fill" : "regular"}
          className={value <= rating ? "text-amber-400" : "text-slate-300"}
        />
      ))}
    </div>
  );
}

function TransactionCard({ item }) {
  return (
    <li className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <Link href={`/posts/${item.postId}`} className="text-sm font-bold text-slate-800 hover:underline">
          {item.postTitle}
        </Link>
        <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-600">
          {STATUS_LABEL[item.status] ?? item.status}
        </span>
      </div>
      <p className="mt-1 text-xs text-slate-500">상대방: {item.counterpartEmail}</p>
      {item.review && (
        <div className="mt-2 rounded-lg bg-slate-50 p-2.5">
          <StarRow rating={item.review.rating} />
          <p className="mt-1 text-xs text-slate-600 line-clamp-2">{item.review.content}</p>
        </div>
      )}
    </li>
  );
}

function ReviewCard({ review, showTarget }) {
  return (
    <li className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <StarRow rating={review.rating} />
        <span className="text-xs text-slate-400">
          {review.createdAt ? new Date(review.createdAt).toLocaleDateString() : ""}
        </span>
      </div>
      <p className="mt-1.5 text-sm text-slate-700 whitespace-pre-line">{review.content}</p>
      <p className="mt-1 text-xs text-slate-400">
        {showTarget ? `대상: ${review.revieweeEmail}` : `작성자: ${review.reviewerEmail}`}
      </p>
      {review.imageUrls?.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-2">
          {review.imageUrls.map((url) => (
            /* eslint-disable-next-line @next/next/no-img-element */
            <img key={url} src={url} alt="후기 이미지" className="h-16 w-16 rounded-md border border-slate-200 object-cover" />
          ))}
        </div>
      )}
    </li>
  );
}

export default function ProfileTabs({ userEmail }) {
  const [activeTab, setActiveTab] = useState("requester");
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    let url;
    if (activeTab === "requester" || activeTab === "repairer") {
      url = `/api/profile/transactions?role=${activeTab}&size=20`;
    } else if (activeTab === "written-reviews") {
      url = `/api/profile/reviews?size=20`;
    } else {
      url = `/api/reviews?revieweeEmail=${encodeURIComponent(userEmail)}&size=20`;
    }

    fetch(url, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error ?? "불러오지 못했습니다.");
        setData(json);
        setError("");
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [activeTab, userEmail]);

  const items =
    activeTab === "requester" || activeTab === "repairer"
      ? data?.items
      : activeTab === "written-reviews"
        ? data?.reviews
        : data?.reviews;

  return (
    <div>
      <div className="flex gap-1 overflow-x-auto border-b border-slate-200">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setActiveTab(tab.key)}
            className={`whitespace-nowrap px-4 py-2.5 text-sm font-semibold transition-colors ${
              activeTab === tab.key
                ? "border-b-2 border-blue-600 text-blue-600"
                : "text-slate-400 hover:text-slate-600"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-4">
        {loading && <p className="text-sm text-slate-400">불러오는 중...</p>}
        {error && <p className="text-sm text-red-600">{error}</p>}

        {!loading && !error && (!items || items.length === 0) && (
          <div className="reference-empty-state" role="status">
            <p>아직 표시할 내역이 없어요.</p>
          </div>
        )}

        {!loading && !error && items?.length > 0 && (
          <ul className="space-y-3">
            {(activeTab === "requester" || activeTab === "repairer") &&
              items.map((item) => <TransactionCard key={item.fixDealId} item={item} />)}
            {activeTab === "written-reviews" &&
              items.map((review) => <ReviewCard key={review.id} review={review} showTarget />)}
            {activeTab === "received-reviews" &&
              items.map((review) => <ReviewCard key={review.id} review={review} />)}
          </ul>
        )}
      </div>
    </div>
  );
}