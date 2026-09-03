"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { PencilSimple, Trash } from "@phosphor-icons/react";

export default function RequestActions({ postId }) {
  const router = useRouter();
  const [deleting, setDeleting] = useState(false);
  const [message, setMessage] = useState("");

  async function handleDelete() {
    if (!confirm("이 수리 요청을 삭제할까요? 삭제하면 되돌릴 수 없어요.")) {
      return;
    }
    setMessage("");
    setDeleting(true);
    try {
      const response = await fetch(`/api/posts/${postId}`, { method: "DELETE" });
      const payload = await response.json();
      if (!response.ok) {
        setMessage(payload.message ?? "삭제하지 못했습니다.");
        return;
      }
      router.push("/requests");
      router.refresh();
    } catch {
      setMessage("서버와 통신할 수 없습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="flex shrink-0 flex-col items-end gap-2">
      <div className="flex gap-2">
        <Link href={`/requests/${postId}/edit`} className="compact-outline-button gap-1.5">
          <PencilSimple size={16} weight="bold" />
          수정
        </Link>
        <button
          type="button"
          onClick={handleDelete}
          disabled={deleting}
          className="compact-outline-button gap-1.5 !border-red-200 !text-red-500 hover:!bg-red-50 disabled:opacity-60"
        >
          <Trash size={16} weight="bold" />
          {deleting ? "삭제 중..." : "삭제"}
        </button>
      </div>
      {message && (
        <p className="form-message form-message-error" role="alert">
          {message}
        </p>
      )}
    </div>
  );
}
