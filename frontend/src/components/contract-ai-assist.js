"use client";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import "./ai-assist.css";

const sourceLabel = source => source === "POST" ? "의뢰 글" : source === "ADOPTED_PROPOSAL" ? "채택 제안"
  : source === "PROPOSAL_AMOUNT" ? "채택 제안 금액" : source === "SUGGESTED_CLAUSE" ? "AI 제안 문구 · 확인 필요"
    : source.startsWith("MESSAGE_") ? `채팅 대화 #${source.slice(8)}` : "직접 입력";

export default function ContractAiAssist({ roomId, baseId, terms, fields, onApply, disabled }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState("");
  const [result, setResult] = useState(null), [instructions, setInstructions] = useState("");
  const [undo, setUndo] = useState({});
  const controller = useRef(null);
  const latest = useRef({ terms, onApply, disabled });
  useLayoutEffect(() => { latest.current = { terms, onApply, disabled }; }, [terms, onApply, disabled]);
  useEffect(() => () => controller.current?.abort(), []);
  const inputKey = JSON.stringify([terms, instructions]);
  async function generate() {
    setBusy(true); setError(""); setResult(null); setUndo({}); controller.current = new AbortController();
    try {
      const response = await fetch(`/api/chat-rooms/${roomId}/contract/ai-draft`, {
        method: "POST", headers: { "Content-Type": "application/json" }, signal: controller.current.signal,
        body: JSON.stringify({ baseId, currentTerms: Object.fromEntries(Object.entries(terms).map(([k,v]) => [k, v == null ? "" : String(v)])), instructions }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "계약 초안을 만들지 못했습니다.");
      if (controller.current.signal.aborted) return;
      const current = latest.current;
      if (current.disabled) { setError("계약 상태가 변경되어 자동 입력하지 않았습니다. 최신 계약을 확인해주세요."); return; }
      const updated = { ...current.terms }, previous = {}, skipped = [];
      for (const [field] of fields) {
        const value = data.suggestedTerms[field];
        if (value == null || value === "") continue;
        if (current.terms[field] !== terms[field]) { skipped.push(field); continue; }
        previous[field] = { before: current.terms[field], applied: value };
        updated[field] = value; current.onApply(field, value);
      }
      setUndo(previous);
      setResult({ data, skipped, inputKey: JSON.stringify([updated, instructions]) });
    } catch (failure) { if (failure.name !== "AbortError") setError(failure.message); }
    finally { setBusy(false); }
  }
  return <section className="ai-assist" aria-label="AI 계약 초안">
    <h3>대화를 정리해 계약 초안 채우기</h3>
    <p>의뢰 글·채택 제안·현재 입력과 이 채팅방의 텍스트 대화를 Gemini에 보내 계약 항목을 자동으로 채웁니다. 이미지·동영상·삭제된 메시지는 제외하고, 금액은 채택된 제안에서 가져옵니다.</p>
    <label>추가 요청 (선택)<textarea maxLength={2000} rows={2} value={instructions} onChange={e => setInstructions(e.target.value)} placeholder="예: 작업에서 제외되는 사항도 구분해주세요." /></label>
    <button type="button" disabled={busy || disabled} onClick={generate}>{busy ? "대화를 정리하고 있어요…" : "AI로 초안 작성"}</button>
    {busy && <span role="status">분석 중 직접 수정한 항목은 유지합니다.</span>}
    {error && <p role="alert" className="ai-error">{error}</p>}
    {result && <div>
      <p role="status">텍스트 대화 {result.data.messageCount}개를 참고해 계약 항목을 자동 입력했습니다. 내용을 확인하고 수정해주세요.</p>
      {result.skipped.length > 0 && <p className="ai-warning">분석 중 수정한 {result.skipped.map(key => fields.find(f => f[0] === key)?.[1] || key).join(", ")}은 유지했습니다.</p>}
      {result.inputKey !== inputKey && <p className="ai-warning">생성 후 입력이 바뀌었습니다. 아래는 생성 당시의 참고 내용입니다.</p>}
      <p>추가 확인·입력 필요: {result.data.missingFields.filter(key => !terms[key]).map(key => fields.find(f => f[0] === key)?.[1] || key).join(", ") || "각 항목을 확인해주세요"}</p>
      <ul>{[...result.data.conflicts, ...result.data.warnings].map((text, i) => <li key={i}>{text}</li>)}</ul>
      <details><summary>작성 근거 및 되돌리기</summary>
        {fields.map(([field, label]) => {
          const value = result.data.suggestedTerms[field], source = result.data.fieldSources[field];
          return value ? <div key={field} className="ai-field"><strong>{label}</strong><p>{value}</p>
            <small>{sourceLabel(source.sourceId)}{source.quote ? `: “${source.quote}”` : ""}</small>
            {undo[field] && terms[field] === undo[field].applied && <button type="button" disabled={disabled} onClick={() => {
              onApply(field, undo[field].before); setUndo(previous => { const next = { ...previous }; delete next[field]; return next; });
            }}>자동 입력 되돌리기</button>}</div> : null;
        })}
      </details>
      <small>내용을 확인한 후 초안을 저장하고 양측이 서명해주세요.</small>
    </div>}
  </section>;
}
