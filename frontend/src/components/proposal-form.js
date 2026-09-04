"use client";

import { useState } from "react";
import { X, Wrench } from "@phosphor-icons/react";
import { useRouter } from "next/navigation";

export default function ProposalForm({ postId }) {
  const [isOpen, setIsOpen] = useState(false);
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const router = useRouter();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const response = await fetch(`/api/posts/${postId}/proposals`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ content }),
      });

      if (response.ok) {
        alert("수리 제안이 성공적으로 등록되었습니다.");
        setContent("");
        setIsOpen(false);
        router.refresh(); // 3. Next.js 서버 컴포넌트 데이터 재요청 및 화면 갱신
      } else {
        alert("수리 제안 등록에 실패했습니다.");
      }
    } catch {
      alert("서버 연결에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 py-3.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 transition"
      >
        <Wrench size={18} weight="bold" />
        수리 제안하기
      </button>

      {isOpen && (
        <div 
          className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm transition-opacity" 
          onClick={() => setIsOpen(false)} 
        />
      )}

      <div
        className={`fixed inset-x-0 bottom-0 z-50 transform rounded-t-3xl bg-white p-6 shadow-2xl transition-transform duration-300 ease-out ${
          isOpen ? "translate-y-0" : "translate-y-full"
        }`}
      >
        <div className="mx-auto mb-4 h-1.5 w-12 rounded-full bg-slate-200" />
        
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-extrabold text-slate-900">이웃에게 수리 제안 남기기</h3>
          <button onClick={() => setIsOpen(false)} className="rounded-full p-1 text-slate-400 hover:bg-slate-100">
            <X size={20} weight="bold" />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="수리 가능 일정, 예상 비용 등 구체적인 제안 내용을 작성해주세요."
            className="w-full h-32 resize-none rounded-xl border border-slate-200 p-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
            required
          />
          <button
            type="submit"
            disabled={submitting}
            className="mt-4 w-full rounded-xl bg-blue-600 py-3.5 text-center text-sm font-semibold text-white shadow-sm hover:bg-blue-700 transition disabled:opacity-50"
          >
            {submitting ? "제안 전송 중..." : "제안 보내기"}
          </button>
        </form>
      </div>
    </>
  );
}