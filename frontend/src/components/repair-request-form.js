"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Wrench } from "@phosphor-icons/react";
import Link from "next/link";

export default function RepairRequestForm({ postId, initialValue }) {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const isEdit = Boolean(postId);

  async function submitRequest(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");

    const formData = new FormData(event.currentTarget);

    try {
      const response = await fetch(isEdit ? `/api/posts/${postId}` : "/api/posts", {
        method: isEdit ? "PATCH" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: formData.get("title"),
          content: formData.get("content"),
        }),
      });
      const payload = await response.json();

      if (!response.ok) {
        setMessage(payload.message ?? "수리 요청을 등록하지 못했습니다.");
        return;
      }

      router.push(isEdit ? `/requests/${postId}` : `/requests/${payload.id}`);
      router.refresh();
    } catch {
      setMessage("수리 요청 서버와 통신할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="repair-form-card">
      <Link href="/requests" className="repair-form-back"><ArrowLeft size={18} />목록으로 돌아가기</Link>
      <span className="repair-form-icon"><Wrench size={30} weight="duotone" /></span>
      <h1>{isEdit ? "수리 요청 수정" : "수리 요청하기"}</h1>
      <p>{isEdit ? "내용을 고치고 저장하면 바로 반영돼요." : "어떤 도움이 필요한지 이웃이 이해하기 쉽게 알려주세요."}</p>

      <form onSubmit={submitRequest} className="repair-form">
        <label className="form-field">
          <span>제목</span>
          <input name="title" type="text" maxLength={100} required defaultValue={initialValue?.title} placeholder="예: 세면대 수도꼭지에서 물이 새요" />
        </label>
        <label className="form-field">
          <span>요청 내용</span>
          <textarea name="content" required rows={7} defaultValue={initialValue?.content} placeholder="문제가 발생한 상황과 필요한 도움을 자세히 적어주세요." />
        </label>

        {message && <div className="form-message form-message-error" role="alert">{message}</div>}

        <button type="submit" className="primary-button w-full justify-center" disabled={submitting}>
          {submitting ? "저장 중..." : isEdit ? "수정 완료" : "수리 요청 등록"}
        </button>
      </form>
    </section>
  );
}