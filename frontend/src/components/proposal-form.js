"use client";

import { useEffect, useState } from "react";
import { X, Wrench, FileText } from "@phosphor-icons/react";
import { useRouter } from "next/navigation";

export default function ProposalForm({ postId }) {
  const [isOpen, setIsOpen] = useState(false);
  const [estimatedPrice, setEstimatedPrice] = useState("");
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [myResume, setMyResume] = useState(null); // null = 아직 확인 전, false = 없음, 객체 = 있음
  const [attachResume, setAttachResume] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (!isOpen || myResume !== null) return;
    const controller = new AbortController();
    fetch("/api/resume", { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        if (res.status === 404 || res.status === 400) {
          setMyResume(false);
          return;
        }
        const json = await res.json();
        setMyResume(res.ok ? json : false);
      })
      .catch(() => {
        if (!controller.signal.aborted) setMyResume(false);
      });
    return () => controller.abort();
  }, [isOpen, myResume]);

  function goWriteResume() {
    if (window.confirm("아직 작성한 이력서가 없어요. 프로필 페이지로 이동해서 작성하시겠어요?")) {
      router.push("/profile");
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const response = await fetch(`/api/posts/${postId}/proposals`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ 
          estimatedPrice: Number(estimatedPrice), 
          content,
          attachResume: Boolean(myResume) && attachResume,
        }),
      });

      if (response.ok) {
        alert("수리 제안이 성공적으로 등록되었습니다.");
        setEstimatedPrice("");
        setContent("");
        setIsOpen(false);
        router.refresh();
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

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">희망 견적 금액 (원)</label>
            <input
              type="number"
              value={estimatedPrice}
              onChange={(e) => setEstimatedPrice(e.target.value)}
              placeholder="예: 50000"
              className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">제안 내용</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="수리 가능 일정, 부품 교체 포함 여부 등 구체적인 제안 내용을 작성해주세요."
              className="w-full h-28 resize-none rounded-xl border border-slate-200 p-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
              required
            />
          </div>

          {myResume === false && (
            <button
              type="button"
              onClick={goWriteResume}
              className="flex w-full items-center gap-2 rounded-xl border border-dashed border-slate-300 p-3 text-left text-xs text-slate-500 hover:border-blue-300 hover:text-blue-600"
            >
              <FileText size={18} weight="duotone" />
              작성한 이력서가 없어요. 프로필에서 이력서를 작성해보세요.
            </button>
          )}

          {myResume && (
            <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={attachResume}
                onChange={(e) => setAttachResume(e.target.checked)}
              />
              <span className="flex-1">
                <span className="font-semibold">&quot;{myResume.headline}&quot;</span> 이력서를 이 제안에 보여주기
              </span>
            </label>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-xl bg-blue-600 py-3.5 text-center text-sm font-semibold text-white shadow-sm hover:bg-blue-700 transition disabled:opacity-50"
          >
            {submitting ? "제안 전송 중..." : "제안 보내기"}
          </button>
        </form>
      </div>
    </>
  );
}