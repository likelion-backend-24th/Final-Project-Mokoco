"use client";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import "./ai-assist.css";

export default function PostAiAssist({ files, values, categories, onApply, onDraftCreated }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState("");
  const [result, setResult] = useState(null);
  const [undo, setUndo] = useState({});
  const controller = useRef(null);
  const inFlight = useRef(false);
  const latest = useRef({ values, files, onApply, onDraftCreated });
  useLayoutEffect(() => { latest.current = { values, files, onApply, onDraftCreated }; }, [values, files, onApply, onDraftCreated]);
  useEffect(() => () => controller.current?.abort(), []);
  const inputKey = JSON.stringify([values, files.map(f => [f.name, f.size, f.lastModified])]);
  async function analyze() {
    if (inFlight.current) return;
    if (!files.length) { setError("분석할 사진을 새로 첨부해주세요."); return; }
    if (files.length > 5 || files.some(f => f.size > 5 * 1024 * 1024) || files.reduce((n, f) => n + f.size, 0) > 15 * 1024 * 1024) {
      setError("AI 분석은 최대 5장, 장당 5MB, 합계 15MB까지 가능합니다."); return;
    }
    inFlight.current = true;
    setBusy(true); setError(""); setUndo({});
    controller.current = new AbortController();
    const data = new FormData();
    files.forEach(file => data.append("images", file));
    Object.entries(result?.inputKey === inputKey ? result.requestValues : values).forEach(([key, value]) => data.append(key, value));
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
      const updated = { ...current.values }, previous = {}, skipped = [];
      for (const field of ["title", "content", "category"]) {
        const value = payload.suggestion[field];
        if (current.values[field] !== values[field]) { skipped.push(field); continue; }
        previous[field] = undo[field]?.applied === current.values[field]
          ? { before: undo[field].before, applied: value } : { before: current.values[field], applied: value };
        updated[field] = value;
        current.onApply(field, value);
      }
      setUndo(previous);
      setResult({ data: payload, skipped, requestValues: result?.inputKey === inputKey ? result.requestValues : values, inputKey: JSON.stringify([updated, files.map(f => [f.name, f.size, f.lastModified])]) });
    } catch (failure) { if (failure.name !== "AbortError") setError(failure.message); }
    finally { inFlight.current = false; setBusy(false); }
  }
  const labels = { title: "제목", content: "내용", category: "카테고리" };
  return <section className="ai-assist" aria-label="사진으로 작성 도움">
    <p>선택한 사진과 작성 내용을 Gemini에 보내 제목·내용·카테고리를 자동으로 채웁니다. JPEG·PNG·WebP, 장당 5MB까지 지원합니다. 사진 속 개인정보를 확인해주세요.</p>
    <button type="button" disabled={busy} onClick={analyze}>{busy ? "사진을 살펴보고 있어요…" : "사진 분석하기"}</button>
    {busy && <span role="status">직접 작성하며 기다려도 괜찮아요.</span>}
    {error && <p role="alert" className="ai-error">{error} 직접 등록할 수 있습니다.</p>}
    {result && <div>
      <p role="status">{result.skipped.length === 3 ? "분석 중 수정한 입력을 유지했습니다." : "분석 결과를 입력란에 자동으로 채웠습니다. 내용을 확인하고 수정해주세요."}</p>
      {result.skipped.length > 0 && <p className="ai-warning">분석 중 수정한 {result.skipped.map(field => labels[field]).join(", ")}은 그대로 유지했습니다.</p>}
      {result.inputKey !== inputKey && <p className="ai-warning">분석 후 입력이 바뀌었습니다. 아래는 이전 입력을 바탕으로 한 제안입니다.</p>}
      {Object.entries(result.data.suggestion).map(([field, value]) => <div className="ai-field" key={field}>
        <strong>{labels[field]}</strong><p>{field === "category" ? categories.find(c => c.value === value)?.label : value}</p>
        {undo[field] && values[field] === undo[field].applied && <button type="button" onClick={() => {
          onApply(field, undo[field].before); setUndo(previous => { const next = { ...previous }; delete next[field]; return next; });
        }}>적용 되돌리기</button>}
      </div>)}
      <small>사진으로 확인한 참고 정보입니다. 내용은 직접 확인한 뒤 등록해주세요.</small>
    </div>}
  </section>;
}
