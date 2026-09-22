"use client";

import { useEffect, useRef, useState } from "react";
import "./ai-assist.css";

const MAX_PROMPT_LENGTH = 500;

export default function PostAiAssist({ files, values, categories, onApply }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);
  const [selection, setSelection] = useState(null);
  const [prompt, setPrompt] = useState("");
  const controller = useRef(null);
  const inFlight = useRef(false);

  useEffect(() => () => controller.current?.abort(), []);

  async function request(path, options) {
    if (inFlight.current) return null;
    inFlight.current = true;
    setBusy(true);
    setError("");
    controller.current = new AbortController();
    try {
      const response = await fetch(path, { method: "POST", signal: controller.current.signal, ...options });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.message || "AI 요청에 실패했습니다.");
      return payload;
    } catch (failure) {
      if (failure.name !== "AbortError") setError(failure.message);
      return null;
    } finally {
      inFlight.current = false;
      setBusy(false);
    }
  }

  async function analyze() {
    if (!files.length) {
      setError("분석할 사진을 새로 첨부해주세요.");
      return;
    }
    if (files.length > 5 || files.some((file) => file.size > 5 * 1024 * 1024)
      || files.reduce((total, file) => total + file.size, 0) > 15 * 1024 * 1024) {
      setError("AI 분석은 최대 5장, 장당 5MB, 합계 15MB까지 가능합니다.");
      return;
    }

    const data = new FormData();
    files.forEach((file) => data.append("images", file));
    Object.entries(values).forEach(([key, value]) => data.append(key, value));
    const payload = await request("/api/ai/post-draft", { body: data });
    if (payload) {
      setResult(payload);
      setSelection(null);
      setPrompt("");
    }
  }

  function selectContent(event) {
    const start = event.currentTarget.selectionStart;
    const end = event.currentTarget.selectionEnd;
    const selectedText = event.currentTarget.value.slice(start, end);
    setSelection(selectedText.trim() ? { start, end, text: selectedText } : null);
  }

  async function revise() {
    if (!selection) {
      setError("AI 본문에서 수정할 문장을 먼저 드래그해주세요.");
      return;
    }
    if (!prompt.trim()) {
      setError("선택한 문장을 어떻게 고칠지 입력해주세요.");
      return;
    }

    const payload = await request(`/api/ai/post-drafts/${result.draftId}/revisions`, {
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        selectionStart: selection.start,
        selectionEnd: selection.end,
        selectedText: selection.text,
        prompt: prompt.trim(),
      }),
    });
    if (payload) {
      setResult(payload);
      setSelection(null);
      setPrompt("");
    }
  }

  const suggestion = result?.suggestion;
  const categoryLabel = suggestion
    ? categories.find((category) => category.value === suggestion.category)?.label || suggestion.category
    : "";

  return (
    <section className="ai-assist" aria-label="사진으로 작성 도움">
      <h3>사진으로 작성 도움받기</h3>
      <p>JPEG·PNG·WebP 사진과 현재 입력을 바탕으로 초안을 만듭니다. 사진 속 개인정보를 확인하고, 결과는 검토한 뒤 필요한 항목만 적용하세요.</p>
      <button type="button" disabled={busy} onClick={analyze}>
        {busy && !result ? "사진을 살펴보고 있어요…" : "사진 분석하기"}
      </button>
      {busy && <span role="status">직접 작성하며 기다려도 괜찮아요.</span>}
      {error && <p role="alert" className="ai-error">{error} 직접 작성할 수도 있습니다.</p>}

      {suggestion && (
        <div className="ai-result">
          <div className="ai-field">
            <strong>제목</strong><p>{suggestion.title}</p>
            <button type="button" onClick={() => onApply("title", suggestion.title)}>제목 사용</button>
          </div>
          <div className="ai-field">
            <strong>카테고리</strong><p>{categoryLabel}</p>
            <button type="button" onClick={() => onApply("category", suggestion.category)}>카테고리 사용</button>
          </div>
          <div className="ai-field">
            <strong>내용</strong>
            <textarea className="ai-content-preview" value={suggestion.content} readOnly rows={7}
              onSelect={selectContent} aria-label="AI 초안 내용에서 수정할 문장 선택" />
            <button type="button" onClick={() => onApply("content", suggestion.content)}>내용 사용</button>
          </div>

          <div className="ai-revision">
            <strong>선택한 문장만 AI로 수정</strong>
            <p>{selection ? `“${selection.text}”` : "위 내용에서 수정할 문장을 드래그해주세요."}</p>
            <textarea value={prompt} maxLength={MAX_PROMPT_LENGTH}
              onChange={(event) => setPrompt(event.target.value)}
              placeholder="예: 고장 증상이 더 구체적으로 보이게 고쳐줘" aria-label="선택 문장 수정 요청" />
            <small>{prompt.length}/{MAX_PROMPT_LENGTH}자 · 남은 수정 {result.remainingRetries}회</small>
            <button type="button" disabled={busy || !selection || !prompt.trim() || result.remainingRetries <= 0} onClick={revise}>
              {busy ? "선택 문장을 수정하고 있어요…" : "선택 문장 수정"}
            </button>
          </div>
          <small>AI는 선택한 내용만 바꾸며 제목과 카테고리는 재생성하지 않습니다. 결과를 확인하고 등록해주세요.</small>
        </div>
      )}
    </section>
  );
}
