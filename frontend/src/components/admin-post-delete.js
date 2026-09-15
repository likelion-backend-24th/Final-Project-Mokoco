"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ShieldWarning } from "@phosphor-icons/react";

// 작성자가 아니어도 관리자 권한으로 글을 삭제할 수 있는 버튼. /posts/[id] 에서 isAdmin && !isMine 일 때만 노출.
export default function AdminPostDelete({ postId }) {
  const router = useRouter();
  const [deleting, setDeleting] = useState(false);
  const [message, setMessage] = useState("");

  async function handleDelete() {
    if (!confirm("[관리자 권한] 이 글을 삭제하시겠습니까? 작성자에게 안내 없이 즉시 삭제됩니다.")) return;
    setMessage("");
    setDeleting(true);
    try {
      const response = await fetch(`/api/admin/posts/${postId}`, { method: "DELETE" });
      if (!response.ok) {
        const payload = await response.json().catch(() => null);
        setMessage(payload?.error || "삭제하지 못했습니다.");
        return;
      }
      router.push("/posts");
      router.refresh();
    } catch {
      setMessage("서버와 통신할 수 없습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="flex shrink-0 flex-col items-end gap-2">
      <button
        type="button"
        onClick={handleDelete}
        disabled={deleting}
        className="compact-outline-button gap-1.5 !border-red-300 !text-red-600 hover:!bg-red-50 disabled:opacity-60"
      >
        <ShieldWarning size={16} weight="bold" />
        {deleting ? "삭제 중..." : "관리자 삭제"}
      </button>
      {message && (
        <p className="form-message form-message-error" role="alert">
          {message}
        </p>
      )}
    </div>
  );
}
