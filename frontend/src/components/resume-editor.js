"use client";

import { useEffect, useState } from "react";
import { Briefcase, FileText, PencilSimple, Plus, Trash, X } from "@phosphor-icons/react";

const MAX_SKILLS = 10;
const MAX_CAREERS = 10;

// "2020.03 - 2023.06" / "2020.03 - 현재" 형태의 문자열을 월 입력값(YYYY-MM)으로 되돌린다.
function parsePeriod(period) {
  if (!period) return { start: "", end: "", ongoing: false };
  const match = period.match(/^(\d{4})\.(\d{2})\s*-\s*(현재|(\d{4})\.(\d{2}))$/);
  if (!match) return { start: "", end: "", ongoing: false };
  const start = `${match[1]}-${match[2]}`;
  if (match[3] === "현재") return { start, end: "", ongoing: true };
  return { start, end: `${match[4]}-${match[5]}`, ongoing: false };
}

// 월 입력값(YYYY-MM)을 "2020.03 - 2023.06" / "2020.03 - 현재" 문자열로 조합한다.
function formatPeriod(start, end, ongoing) {
  if (!start) return "";
  const fmt = (ym) => ym.replace("-", ".");
  return ongoing ? `${fmt(start)} - 현재` : end ? `${fmt(start)} - ${fmt(end)}` : fmt(start);
}

function emptyCareerRow() {
  return { start: "", end: "", ongoing: false, description: "" };
}

function emptyForm(resume) {
  return {
    headline: resume?.headline ?? "",
    introduction: resume?.introduction ?? "",
    skills: resume?.skills ?? [],
    careers: resume?.careers?.length
      ? resume.careers.map((c) => ({ ...parsePeriod(c.period), description: c.description }))
      : [emptyCareerRow()],
  };
}

export default function ResumeEditor() {
  const [resume, setResume] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(emptyForm(null));
  const [skillInput, setSkillInput] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [refreshToken, setRefreshToken] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/resume", { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        if (res.status === 404 || res.status === 400) {
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
  }, [refreshToken]);

  function startCreate() {
    setForm(emptyForm(null));
    setSkillInput("");
    setError("");
    setEditing(true);
  }

  function startEdit() {
    setForm(emptyForm(resume));
    setSkillInput("");
    setError("");
    setEditing(true);
  }

  function addSkill() {
    const value = skillInput.trim();
    if (!value) return;
    if (form.skills.length >= MAX_SKILLS) {
      setError(`전문 분야는 최대 ${MAX_SKILLS}개까지 등록할 수 있어요.`);
      return;
    }
    if (form.skills.includes(value)) {
      setSkillInput("");
      return;
    }
    setForm((current) => ({ ...current, skills: [...current.skills, value] }));
    setSkillInput("");
  }

  function removeSkill(index) {
    setForm((current) => ({ ...current, skills: current.skills.filter((_, i) => i !== index) }));
  }

  function addCareerRow() {
    if (form.careers.length >= MAX_CAREERS) return;
    setForm((current) => ({ ...current, careers: [...current.careers, emptyCareerRow()] }));
  }

  function removeCareerRow(index) {
    setForm((current) => ({ ...current, careers: current.careers.filter((_, i) => i !== index) }));
  }

  function updateCareer(index, field, value) {
    setForm((current) => ({
      ...current,
      careers: current.careers.map((career, i) => (i === index ? { ...career, [field]: value } : career)),
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!form.headline.trim()) {
      setError("한 줄 소개를 입력해주세요.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const careers = form.careers
        .filter((c) => c.start || c.description.trim())
        .map((c) => ({ period: formatPeriod(c.start, c.end, c.ongoing), description: c.description }));
      const res = await fetch("/api/resume", {
        method: resume ? "PATCH" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          headline: form.headline,
          introduction: form.introduction,
          skills: form.skills,
          careers,
        }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error ?? "저장하지 못했습니다.");
        return;
      }
      setEditing(false);
      setRefreshToken((token) => token + 1);
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm("이력서를 삭제할까요? 삭제하면 되돌릴 수 없어요.")) return;
    setSubmitting(true);
    setError("");
    try {
      const res = await fetch("/api/resume", { method: "DELETE" });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error ?? "삭제하지 못했습니다.");
        return;
      }
      setResume(null);
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <p className="text-sm text-slate-400">이력서 확인 중...</p>;

  if (editing) {
    return (
      <form onSubmit={handleSubmit} className="rounded-xl border border-slate-200 bg-white p-5">
        <p className="mb-4 text-sm font-bold text-slate-800">{resume ? "이력서 수정" : "이력서 작성"}</p>

        <label className="block text-xs font-semibold text-slate-500">한 줄 소개</label>
        <input
          value={form.headline}
          onChange={(event) => setForm((c) => ({ ...c, headline: event.target.value }))}
          placeholder="예) 10년차 가전 수리 전문가"
          maxLength={100}
          className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400"
        />

        <label className="mt-4 block text-xs font-semibold text-slate-500">전문 분야</label>
        <div className="mt-1 flex flex-wrap gap-1.5">
          {form.skills.map((skill, index) => (
            <span
              key={`${skill}-${index}`}
              className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700"
            >
              {skill}
              <button type="button" onClick={() => removeSkill(index)} aria-label={`${skill} 제거`}>
                <X size={12} weight="bold" />
              </button>
            </span>
          ))}
        </div>
        <div className="mt-1.5 flex gap-1.5">
          <input
            value={skillInput}
            onChange={(event) => setSkillInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.nativeEvent.isComposing) {
                event.preventDefault();
                addSkill();
              }
            }}
            placeholder="예) 세탁기 (Enter로 추가)"
            className="flex-1 rounded-lg border border-slate-200 px-3 py-1.5 text-sm outline-none focus:border-blue-400"
          />
          <button
            type="button"
            onClick={addSkill}
            className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50"
          >
            추가
          </button>
        </div>

        <label className="mt-4 block text-xs font-semibold text-slate-500">경력 사항</label>
        <div className="mt-1 space-y-2">
          {form.careers.map((career, index) => (
            <div key={index} className="rounded-lg border border-slate-100 p-2.5">
              <div className="flex flex-wrap items-center gap-1.5">
                <input
                  type="month"
                  value={career.start}
                  onChange={(event) => updateCareer(index, "start", event.target.value)}
                  className="rounded-lg border border-slate-200 px-2 py-1.5 text-xs outline-none focus:border-blue-400"
                />
                <span className="text-xs text-slate-400">~</span>
                <input
                  type="month"
                  value={career.end}
                  disabled={career.ongoing}
                  onChange={(event) => updateCareer(index, "end", event.target.value)}
                  className="rounded-lg border border-slate-200 px-2 py-1.5 text-xs outline-none focus:border-blue-400 disabled:bg-slate-50 disabled:text-slate-300"
                />
                <label className="flex items-center gap-1 text-xs text-slate-500">
                  <input
                    type="checkbox"
                    checked={career.ongoing}
                    onChange={(event) => updateCareer(index, "ongoing", event.target.checked)}
                  />
                  현재 재직중
                </label>
                <button
                  type="button"
                  onClick={() => removeCareerRow(index)}
                  aria-label="경력 삭제"
                  className="ml-auto rounded-lg px-2 text-slate-400 hover:bg-slate-50"
                >
                  <Trash size={14} />
                </button>
              </div>
              <input
                value={career.description}
                onChange={(event) => updateCareer(index, "description", event.target.value)}
                placeholder="내용 (예: OO전자서비스 가전 수리 담당)"
                className="mt-1.5 w-full rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs outline-none focus:border-blue-400"
              />
            </div>
          ))}
        </div>
        {form.careers.length < MAX_CAREERS && (
          <button
            type="button"
            onClick={addCareerRow}
            className="mt-1.5 inline-flex items-center gap-1 text-xs font-semibold text-blue-600 hover:underline"
          >
            <Plus size={12} weight="bold" /> 경력 추가
          </button>
        )}

        <label className="mt-4 block text-xs font-semibold text-slate-500">자기소개</label>
        <textarea
          value={form.introduction}
          onChange={(event) => setForm((c) => ({ ...c, introduction: event.target.value }))}
          placeholder="경력, 강점, 작업 방식 등을 자유롭게 소개해주세요."
          rows={5}
          maxLength={2000}
          className="mt-1 w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 outline-none focus:border-blue-400"
        />

        {error && <p className="mt-2 text-xs text-red-600">{error}</p>}

        <div className="mt-4 flex gap-2">
          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-blue-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {submitting ? "저장 중..." : "저장"}
          </button>
          <button
            type="button"
            onClick={() => setEditing(false)}
            className="rounded-lg border border-slate-200 px-4 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50"
          >
            취소
          </button>
        </div>
      </form>
    );
  }

  if (!resume) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-center">
        <FileText size={28} weight="duotone" className="mx-auto text-slate-300" />
        <p className="mt-2 text-sm text-slate-500">아직 작성한 이력서가 없어요.</p>
        <p className="mt-0.5 text-xs text-slate-400">수리자로 활동하신다면 이력서로 자신을 어필해보세요.</p>
        <button
          type="button"
          onClick={startCreate}
          className="mt-3 rounded-lg bg-blue-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-blue-700"
        >
          이력서 작성하기
        </button>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
      <div className="bg-gradient-to-r from-blue-600 to-blue-500 p-5 text-white">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-semibold text-blue-100">{resume.userEmail}</p>
            <h3 className="mt-0.5 text-lg font-extrabold">{resume.headline}</h3>
          </div>
          <div className="flex gap-1">
            <button type="button" onClick={startEdit} aria-label="수정" className="rounded-full p-1.5 text-blue-100 hover:bg-white/15">
              <PencilSimple size={16} />
            </button>
            <button
              type="button"
              onClick={handleDelete}
              disabled={submitting}
              aria-label="삭제"
              className="rounded-full p-1.5 text-blue-100 hover:bg-white/15"
            >
              <Trash size={16} />
            </button>
          </div>
        </div>
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

        {error && <p className="mt-2 text-xs text-red-600">{error}</p>}

        <p className="mt-4 text-xs text-slate-400">
          {resume.updatedAt ? `${new Date(resume.updatedAt).toLocaleDateString()} 수정됨` : ""}
        </p>
      </div>
    </div>
  );
}