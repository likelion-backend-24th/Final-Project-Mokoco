"use client";

import { useEffect, useState } from "react";
import { Briefcase, X } from "@phosphor-icons/react";

export default function ResumeViewModal({ userId, onClose }) {
  const [resume, setResume] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/resume/${encodeURIComponent(userId)}`, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error ?? "이력서를 불러오지 못했습니다.");
        setResume(json);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [userId]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose} role="presentation">
      <div
        className="max-h-[80vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-slate-100 p-4">
          <h3 className="text-sm font-bold text-slate-800">이력서</h3>
          <button type="button" onClick={onClose} aria-label="닫기" className="rounded-full p-1 text-slate-400 hover:bg-slate-100">
            <X size={18} />
          </button>
        </div>

        <div className="p-4">
          {loading && <p className="text-sm text-slate-400">불러오는 중...</p>}
          {error && <p className="text-sm text-red-600">{error}</p>}

          {resume && (
            <>
              <p className="text-xs text-slate-400">{resume.userId}</p>
              <h4 className="mt-0.5 text-base font-extrabold text-slate-800">{resume.headline}</h4>

              {resume.skills?.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {resume.skills.map((skill) => (
                    <span key={skill} className="rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700">
                      {skill}
                    </span>
                  ))}
                </div>
              )}

              {resume.careers?.length > 0 && (
                <div className="mt-4">
                  <p className="mb-2 flex items-center gap-1.5 text-xs font-bold text-slate-500">
                    <Briefcase size={14} weight="bold" /> 경력 사항
                  </p>
                  <ul className="space-y-2 border-l-2 border-slate-100 pl-3">
                    {resume.careers.map((career, index) => (
                      <li key={index}>
                        <p className="text-xs font-semibold text-slate-400">{career.period}</p>
                        <p className="text-sm text-slate-700">{career.description}</p>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {resume.introduction && (
                <div className="mt-4">
                  <p className="mb-1.5 text-xs font-bold text-slate-500">자기소개</p>
                  <p className="whitespace-pre-line text-sm text-slate-600">{resume.introduction}</p>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
