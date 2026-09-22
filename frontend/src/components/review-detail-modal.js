"use client";

import { useEffect, useState } from "react";
import { Star, X } from "@phosphor-icons/react";

function StarRow({ rating }) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((value) => (
        <Star
          key={value}
          size={14}
          weight={value <= rating ? "fill" : "regular"}
          className={value <= rating ? "text-amber-400" : "text-slate-300"}
        />
      ))}
    </div>
  );
}

function maskEmail(email) {
  if (!email) return "익명";
  const [name, domain] = email.split("@");
  if (!domain) return email;
  return `${name.slice(0, 2)}***@${domain}`;
}

// postId로 특정 거래 하나에 달린 후기만 보여준다 (그 수리자의 전체 후기 목록이 아님)
export default function ReviewDetailModal({ postId, onClose }) {
  const [review, setReview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/reviews/by-post?postId=${postId}`, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error ?? "후기를 불러오지 못했습니다.");
        setReview(json);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [postId]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="max-h-[80vh] w-full max-w-sm overflow-y-auto rounded-2xl bg-white p-5 shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-800">이 거래의 후기</h3>
          <button type="button" onClick={onClose} aria-label="닫기" className="rounded-full p-1 text-slate-400 hover:bg-slate-100">
            <X size={18} />
          </button>
        </div>

        {loading && <p className="text-sm text-slate-400">불러오는 중...</p>}
        {error && <p className="text-sm text-red-600">{error}</p>}

        {review && (
          <div className="rounded-lg border border-slate-100 p-3">
            <div className="flex items-center justify-between">
              <StarRow rating={review.rating} />
              <span className="text-xs text-slate-400">
                {review.createdAt ? new Date(review.createdAt).toLocaleDateString() : ""}
              </span>
            </div>
            <p className="mt-1.5 text-sm text-slate-700 whitespace-pre-line">{review.content}</p>
            <p className="mt-1 text-xs text-slate-400">{maskEmail(review.reviewerEmail)}</p>

            {review.imageUrls?.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-2">
                {review.imageUrls.map((url) => (
                  /* eslint-disable-next-line @next/next/no-img-element */
                  <img
                    key={url}
                    src={url}
                    alt="후기 이미지"
                    className="h-20 w-20 rounded-md border border-slate-200 object-cover"
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
