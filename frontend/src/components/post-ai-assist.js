"use client";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import "./ai-assist.css";

export default function PostAiAssist({ files, values, onApply, onDraftCreated }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const controller = useRef(null);
  const inFlight = useRef(false);
  const latest = useRef({ values, files, onApply, onDraftCreated });
  useLayoutEffect(() => { latest.current = { values, files, onApply, onDraftCreated }; }, [values, files, onApply, onDraftCreated]);
  useEffect(() => () => controller.current?.abort(), []);
  async function analyze() {
    if (inFlight.current) return;
    if (!files.length) { setError("분석할 사진을 새로 첨부해주세요."); return; }
    if (files.length > 5 || files.some(f => f.size > 5 * 1024 * 1024) || files.reduce((n, f) => n + f.size, 0) > 15 * 1024 * 1024) {
      setError("AI 분석은 최대 5장, 장당 5MB, 합계 15MB까지 가능합니다."); return;
    }
    inFlight.current = true;
    setBusy(true); setError(""); setStatus("");
    controller.current = new AbortController();
    const data = new FormData();
    files.forEach(file => data.append("images", file));
    Object.entries(values).forEach(([key, value]) => data.append(key, value));
    try {
      const response = await fetch("/api/ai/post-draft", { method: "POST", body: data, signal: controller.current.signal });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.message || "분석에 실패했습니다.");
      if (controller.current.signal.aborted) return;
      const current = latest.current;
      if (current.files !== files) {
        setError("분석 중 사진이 바뀌었습니다. 현재 사진으로 다시 분석해주세요."); return;
      }
      if (typeof payload.draftId === "number" && typeof payload.remainingRevisions === "number") {
        current.onDraftCreated?.({ draftId: payload.draftId, remainingRevisions: payload.remainingRevisions });
      }
      // 분석을 시작한 뒤 사용자가 이미 직접 고친 항목은 덮어쓰지 않는다.
      let skipped = 0;
      for (const field of ["title", "content", "category"]) {
        if (current.values[field] !== values[field]) { skipped++; continue; }
        current.onApply(field, payload.suggestion[field]);
      }
      setStatus(skipped === 3 ? "분석 중 수정한 입력을 유지했습니다." : "분석 결과를 제목·내용·카테고리에 반영했습니다. 확인하고 수정해주세요.");
    } catch (failure) { if (failure.name !== "AbortError") setError(failure.message); }
    finally { inFlight.current = false; setBusy(false); }
  }
  return <section className="ai-assist" aria-label="사진으로 작성 도움">
    <p>선택한 사진과 작성 내용을 Gemini에 보내 제목·내용·카테고리를 자동으로 채웁니다. JPEG·PNG·WebP, 장당 5MB까지 지원합니다. 사진 속 개인정보를 확인해주세요.</p>
    <button type="button" disabled={busy} onClick={analyze}>{busy ? "사진을 살펴보고 있어요…" : "사진 분석하기"}</button>
    {busy && <span role="status">직접 작성하며 기다려도 괜찮아요.</span>}
    {error && <p role="alert" className="ai-error">{error} 직접 등록할 수 있습니다.</p>}
    {status && <p role="status">{status}</p>}
  </section>;
}
