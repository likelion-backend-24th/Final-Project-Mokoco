"use client";

import { useState } from "react";
import { Star } from "@phosphor-icons/react";

export default function ReviewForm({ postId, onSubmitted }) {
  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    if (!content.trim()) {
      setError("후기 내용을 입력해주세요.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const res = await fetch("/api/reviews", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ postId, rating, content }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error ?? "후기를 등록하지 못했습니다.");
        return;
      }
      onSubmitted?.();
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-3 w-full rounded-lg border border-slate-200 bg-white p-3">
      <p className="mb-2 text-xs font-semibold text-slate-600">수리자에게 후기를 남겨주세요</p>

      <div className="mb-2 flex gap-1">
        {[1, 2, 3, 4, 5].map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setRating(value)}
            aria-label={`${value}점`}
            className="p-0.5"
          >
            <Star
              size={20}
              weight={value <= rating ? "fill" : "regular"}
              className={value <= rating ? "text-amber-400" : "text-slate-300"}
            />
          </button>
        ))}
      </div>

      <textarea
        value={content}
        onChange={(event) => setContent(event.target.value)}
        placeholder="수리는 어떠셨나요? 다른 분들에게 도움이 될 후기를 남겨주세요."
        rows={3}
        maxLength={1000}
        className="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 outline-none focus:border-blue-400"
      />

      {error && (
        <p role="alert" className="mt-1.5 text-xs text-red-600">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={submitting}
        className="mt-2 rounded-lg bg-blue-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {submitting ? "등록 중..." : "후기 등록"}
      </button>
    </form>
  );
}
