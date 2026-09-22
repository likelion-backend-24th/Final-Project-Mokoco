"use client";

import { useState } from "react";
import { Star, Image as ImageIcon, X } from "@phosphor-icons/react";

const MAX_IMAGES = 5;

export default function ReviewForm({ postId, onSubmitted }) {
  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const [files, setFiles] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  function handleFileChange(event) {
    const picked = Array.from(event.target.files ?? []);
    if (files.length + picked.length > MAX_IMAGES) {
      setError(`이미지는 최대 ${MAX_IMAGES}장까지 첨부할 수 있어요.`);
      event.target.value = "";
      return;
    }
    setError("");
    setFiles((current) => [...current, ...picked]);
    event.target.value = "";
  }

  function removeFile(index) {
    setFiles((current) => current.filter((_, i) => i !== index));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!content.trim()) {
      setError("후기 내용을 입력해주세요.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      // 이미지 업로드는 게이트웨이의 멀티파트 유실 문제를 피해 post-service로 직결되므로(Caddyfile
      // 참고), 백엔드가 기대하는 review(JSON 파트) + images(파일 파트) 형태를 여기서 직접 만든다.
      const formData = new FormData();
      formData.append(
        "review",
        new Blob([JSON.stringify({ postId: Number(postId), rating: Number(rating), content })], {
          type: "application/json",
        }),
      );
      files.forEach((file) => formData.append("images", file, file.name));

      const token = typeof window !== "undefined" ? localStorage.getItem("access_token") : null;
      const res = await fetch("/api/reviews", {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.message ?? "후기를 등록하지 못했습니다.");
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

      {files.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-2">
          {files.map((file, index) => (
            <div key={`${file.name}-${index}`} className="relative">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={URL.createObjectURL(file)}
                alt={file.name}
                className="h-16 w-16 rounded-md border border-slate-200 object-cover"
              />
              <button
                type="button"
                onClick={() => removeFile(index)}
                aria-label="이미지 제거"
                className="absolute -right-1.5 -top-1.5 rounded-full bg-slate-800/80 p-0.5 text-white"
              >
                <X size={12} weight="bold" />
              </button>
            </div>
          ))}
        </div>
      )}

      <label className="mt-2 inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-dashed border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-500 hover:border-blue-400 hover:text-blue-600">
        <ImageIcon size={16} weight="bold" />
        사진 첨부 ({files.length}/{MAX_IMAGES})
        <input type="file" multiple accept="image/*" onChange={handleFileChange} className="hidden" />
      </label>

      {error && (
        <p role="alert" className="mt-1.5 text-xs text-red-600">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={submitting}
        className="mt-2 block rounded-lg bg-blue-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {submitting ? "등록 중..." : "후기 등록"}
      </button>
    </form>
  );
}
