"use client";

import { useEffect, useState } from "react";
import { Briefcase, FileText, X } from "@phosphor-icons/react";

// 다른 사람(주로 제안을 보낸 수리자)의 이력서를 읽기 전용으로 보여준다.
// 내 이력서 작성/수정은 resume-editor.js(프로필 페이지), 이건 조회 전용 모달이다.
export default function ResumeViewModal({ email, onClose }) {
  const [resume, setResume] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/resume/${encodeURIComponent(email)}`, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        if (res.status === 404) {
          setResume(null);
          return;
        }
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
  }, [email]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="max-h-[85vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-slate-100 p-4">
          <h3 className="text-sm font-bold text-slate-800">이력서</h3>
          <button type="button" onClick={onClose} aria-label="닫기" className="rounded-full p-1 text-slate-400 hover:bg-slate-100">
            <X size={18} />
          </button>
        </div>

        {loading && <p className="p-5 text-sm text-slate-400">불러오는 중...</p>}
        {error && <p className="p-5 text-sm text-red-600">{error}</p>}

        {!loading && !error && !resume && (
          <div className="p-6 text-center">
            <FileText size={28} weight="duotone" className="mx-auto text-slate-300" />
            <p className="mt-2 text-sm text-slate-500">아직 작성한 이력서가 없어요.</p>
          </div>
        )}

        {resume && (
          <>
            <div className="bg-gradient-to-r from-blue-600 to-blue-500 p-5 text-white">
              <p className="text-xs font-semibold text-blue-100">{resume.userEmail}</p>
              <h3 className="mt-0.5 text-lg font-extrabold">{resume.headline}</h3>
              {resume.skills?.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {resume.skills.map((skill) => (
                    <span key={skill} className="rounded-full bg-white/20 px-2.5 py-0.5 text-xs font-semibold">
                      {skill}
                    </span>
                  ))}
                </div>
              )}
            </div>

            <div className="p-5">
              {resume.careers?.length > 0 && (
                <div className="mb-4">
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
                <div>
                  <p className="mb-1.5 text-xs font-bold text-slate-500">자기소개</p>
                  <p className="whitespace-pre-line text-sm text-slate-600">{resume.introduction}</p>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
