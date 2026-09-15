"use client";

import { useEffect, useState } from "react";
import { Star } from "@phosphor-icons/react";
import ReviewDetailModal from "@/components/review-detail-modal";

// email: 평균 별점 계산용 (그 수리자의 전체 후기 기준)
// postId: 클릭 시 "이 거래의 후기"만 보여주기 위한 값
export default function RatingBadge({ email, postId }) {
  const [data, setData] = useState(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!email) return;
    const controller = new AbortController();
    fetch(`/api/reviews?revieweeEmail=${encodeURIComponent(email)}&size=1`, {
      signal: controller.signal,
      cache: "no-store",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((json) => {
        if (!controller.signal.aborted) setData(json);
      })
      .catch(() => {});
    return () => controller.abort();
  }, [email]);

  if (!data || data.averageRating == null) return null;

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="inline-flex items-center gap-0.5 text-xs font-semibold text-amber-500 hover:underline"
      >
        <Star size={13} weight="fill" />
        {data.averageRating.toFixed(1)}
      </button>
      {open && <ReviewDetailModal postId={postId} onClose={() => setOpen(false)} />}
    </>
  );
}
