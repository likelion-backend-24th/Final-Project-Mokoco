"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Wrench, Upload, X, Sparkle } from "@phosphor-icons/react";
import Link from "next/link";
import { backendUrl, imageSrc } from "@/lib/backend";
import { plainTextToHtml } from "@/lib/plain-text-to-html";
import PostAiAssist from "./post-ai-assist";
import RichTextEditor from "./rich-text-editor";

const categories = [
  { value: "ELECTRIC_LIGHT", label: "전기·조명" },
  { value: "PLUMBING", label: "배관·설비" },
  { value: "FURNITURE_INSTALL", label: "가구·설치" },
  { value: "HOME_APPLIANCE", label: "가전제품" },
  { value: "DOOR_WINDOW", label: "문·창문" },
  { value: "LIVING_ETC", label: "생활·기타" },
];

const MAX_AI_REVISIONS = 3;

export default function PostForm({ postId, initialValue, accessToken }) {
  const router = useRouter();
  const editorRef = useRef(null);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [title, setTitle] = useState(initialValue?.title || "");
  // 리치텍스트 에디터로 전환하기 전(PLAIN_TEXT)에 쓴 글을 고칠 땐, 원래 줄바꿈이 보이도록
  // HTML로 변환해서 불러온다. 이후 저장하는 순간부터는 항상 HTML로 저장된다.
  const [content, setContent] = useState(
    !initialValue?.content ? ""
      : initialValue.contentFormat === "HTML" ? initialValue.content
      : plainTextToHtml(initialValue.content)
  );

  const [existingImages, setExistingImages] = useState(initialValue?.images || []);
  const [removingImageId, setRemovingImageId] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState(initialValue?.category || "ELECTRIC_LIGHT");
  const isEdit = Boolean(postId);

  // "사진으로 작성 도움받기"로 AI 초안을 한 번 만들면 세션(draftId)이 열리고, 그 안에서
  // 본문 중 선택한 문장만 다시 써달라고 최대 3번까지 요청할 수 있다.
  const [aiDraftId, setAiDraftId] = useState(null);
  const [remainingRevisions, setRemainingRevisions] = useState(MAX_AI_REVISIONS);
  const [aiSelection, setAiSelection] = useState(null);
  const [aiReviseOpen, setAiReviseOpen] = useState(false);
  const [aiInstruction, setAiInstruction] = useState("");
  const [aiRevisionBusy, setAiRevisionBusy] = useState(false);

  const authHeaders = {
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
  };

  // 새로 고른 파일은 아직 서버에 없어 URL이 없으므로, 선택하는 시점(이벤트 핸들러)에
  // 바로 브라우저 메모리 안에서만 보이는 미리보기 URL을 만들어 파일과 함께 들고 있는다
  // (렌더링 도중에 만들면 정리 시점을 놓쳐 메모리에 계속 쌓인다). 제거되거나 폼을 떠날 때
  // 해제한다(revoke).
  const handleFileChange = (e) => {
    if (!e.target.files) return;
    const filesArray = Array.from(e.target.files);

    if (existingImages.length + selectedFiles.length + filesArray.length > 5) {
      setMessage("이미지는 최대 5장까지 등록할 수 있습니다.");
      return;
    }

    const withPreview = filesArray.map((file) => ({ file, previewUrl: URL.createObjectURL(file) }));
    setSelectedFiles((prev) => [...prev, ...withPreview]);
    setMessage("");
  };

  useEffect(() => () => {
    // 함수형 업데이트로 언마운트 시점의 최신 목록을 읽되, 그대로 돌려줘서 리렌더는 일으키지 않는다.
    setSelectedFiles((prev) => { prev.forEach((item) => URL.revokeObjectURL(item.previewUrl)); return prev; });
  }, []);

  const removeNewFile = (index) => {
    setSelectedFiles((prev) => {
      const removed = prev[index];
      if (removed) URL.revokeObjectURL(removed.previewUrl);
      return prev.filter((_, i) => i !== index);
    });
  };

  async function removeExistingImage(image) {
    if (!postId || removingImageId) return;
    setRemovingImageId(image.id);
    setMessage("");
    try {
      const res = await fetch(backendUrl(`/api/posts/${postId}/images/${image.id}`), {
        method: "DELETE",
        headers: authHeaders,
        credentials: "include",
      });
      if (!res.ok) {
        setMessage("이미지를 삭제하지 못했습니다.");
        return;
      }
      setExistingImages((prev) => prev.filter((img) => img.id !== image.id));
    } catch {
      setMessage("이미지를 삭제하지 못했습니다.");
    } finally {
      setRemovingImageId(null);
    }
  }

  function handleSelectionChange(selection) {
    setAiSelection(selection);
  }

  function openAiRevision() {
    if (!aiDraftId) { setMessage("먼저 사진으로 AI 초안을 생성해주세요."); return; }
    if (!aiSelection?.text?.trim()) { setMessage("AI로 다듬을 문장을 먼저 선택해주세요."); return; }
    if (remainingRevisions <= 0) { setMessage("AI 부분 수정 횟수를 모두 사용했습니다."); return; }
    setMessage("");
    setAiInstruction("");
    setAiReviseOpen(true);
  }

  function closeAiRevision() {
    if (aiRevisionBusy) return;
    setAiReviseOpen(false);
    setAiInstruction("");
  }

  async function reviseSelectedText() {
    if (aiRevisionBusy) return;
    if (!aiSelection?.text?.trim()) { setMessage("AI로 다듬을 문장을 선택해주세요."); return; }
    const instruction = aiInstruction.trim();
    if (!instruction) { setMessage("어떻게 고칠지 요청 내용을 입력해주세요."); return; }

    setAiRevisionBusy(true);
    setMessage("");
    try {
      const response = await fetch("/api/ai/post-revise", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          draftId: aiDraftId,
          selectedText: aiSelection.text,
          contextBefore: aiSelection.contextBefore || "",
          contextAfter: aiSelection.contextAfter || "",
          instruction,
        }),
      });
      const payload = await response.json().catch(() => null);
      if (!response.ok) {
        setMessage(payload?.message || "AI 부분 수정에 실패했습니다. 직접 수정해주세요.");
        return;
      }
      const replacement = payload?.replacement;
      if (!replacement?.trim()) {
        setMessage("AI가 수정 결과를 만들지 못했습니다. 다시 시도해주세요.");
        return;
      }
      const replaced = editorRef.current?.replaceSelection?.(replacement, aiSelection);
      if (replaced === false) {
        setMessage("선택한 문장이 바뀌었습니다. 다시 선택해주세요.");
        return;
      }
      if (typeof payload.remainingRevisions === "number") setRemainingRevisions(payload.remainingRevisions);
      setAiReviseOpen(false);
      setAiInstruction("");
      setAiSelection(null);
    } catch {
      setMessage("AI 서버와 통신할 수 없습니다. 입력 내용은 유지됩니다.");
    } finally {
      setAiRevisionBusy(false);
    }
  }

  async function submitPost(event) {
    event.preventDefault();
    // 에디터는 <textarea required>가 아니라서 브라우저가 빈 내용을 막아주지 않는다 —
    // 태그만 있고 글자가 없는 경우(예: "<p></p>")까지 직접 걸러야 한다.
    if (content.replace(/<[^>]*>/g, "").trim().length === 0) {
      setMessage("내용을 입력해주세요.");
      return;
    }
    setSubmitting(true);
    setMessage("");

    const postDto = { title, content, category: selectedCategory, contentFormat: "HTML" };

    try {
      let response;
      if (isEdit) {
        // 수정: 백엔드 PATCH 는 JSON(title/content/category)만 받는다. 이미지는 아래에서 별도 엔드포인트로.
        response = await fetch(backendUrl(`/api/posts/${postId}`), {
          method: "PATCH",
          headers: { "Content-Type": "application/json", ...authHeaders },
          credentials: "include",
          body: JSON.stringify(postDto),
        });
      } else {
        const formData = new FormData();
        formData.append("post", new Blob([JSON.stringify(postDto)], { type: "application/json" }));
        selectedFiles.forEach(({ file }) => formData.append("images", file));
        response = await fetch(backendUrl("/api/posts"), {
          method: "POST",
          headers: authHeaders,
          credentials: "include",
          body: formData,
        });
      }

      if (!response.ok) {
        let msg = "수리 요청을 등록하지 못했습니다.";
        try {
          const p = await response.json();
          msg = p.message ?? msg;
        } catch { /* 본문 없음 */ }
        setMessage(msg);
        return;
      }

      // 수정 화면에서 새로 첨부한 사진이 있으면 이미지 추가 엔드포인트로 별도 업로드
      if (isEdit && selectedFiles.length > 0) {
        const imgForm = new FormData();
        selectedFiles.forEach(({ file }) => imgForm.append("images", file));
        const imgRes = await fetch(backendUrl(`/api/posts/${postId}/images`), {
          method: "POST",
          headers: authHeaders,
          credentials: "include",
          body: imgForm,
        });
        if (!imgRes.ok) {
          setMessage("글 내용은 수정됐지만 사진 추가에 실패했습니다.");
          setSubmitting(false);
          return;
        }
      }

      // PATCH 는 본문 없이 200(Void), POST 는 새 글 id 반환
      const targetId = isEdit ? postId : await response.json();
      router.push(`/posts/${targetId}`);
      router.refresh();
    } catch {
      setMessage("수리 요청 서버와 통신할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="repair-form-card">
      <Link href="/posts" className="repair-form-back"><ArrowLeft size={18} />목록으로 돌아가기</Link>
      <span className="repair-form-icon"><Wrench size={30} weight="duotone" /></span>
      <h1>{isEdit ? "수리 요청 수정" : "수리 요청하기"}</h1>
      <p>{isEdit ? "내용을 고치고 저장하면 바로 반영돼요." : "어떤 도움이 필요한지 이웃이 이해하기 쉽게 알려주세요."}</p>

      <form onSubmit={submitPost} className="repair-form">
        <label className="form-field">
          <span>카테고리</span>
          <select 
            value={selectedCategory} 
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="w-full rounded-xl border border-slate-200 p-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none bg-white"
          >
            {categories.map((cat) => (
              <option key={cat.value} value={cat.value}>
                {cat.label}
              </option>
            ))}
          </select>
        </label>
        
        {/* 제목 입력 */}
        <label className="form-field">
          <span>제목</span>
          <input
            name="title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목을 입력해주세요"
            required
            className="w-full rounded-xl border border-slate-200 p-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
          />
        </label>

        {/* 내용 입력 */}
        <label className="form-field">
          <span>내용</span>
          <RichTextEditor
            ref={editorRef}
            value={content}
            onChange={setContent}
            onSelectionChange={handleSelectionChange}
            placeholder="어떤 도움이 필요한지 자세히 적어주세요"
          />
        </label>

        {/* 문장을 선택하면 AI로 그 부분만 다듬을 수 있다는 배너를 띄운다. */}
        {aiSelection?.text?.trim() && !aiReviseOpen && (
          <div className="flex items-center justify-between gap-3 rounded-xl border border-violet-200 bg-violet-50 p-3">
            <div className="min-w-0">
              <p className="text-sm font-medium text-violet-900">선택한 문장을 AI로 다듬을 수 있어요.</p>
              <p className="mt-1 truncate text-xs text-violet-700">“{aiSelection.text}”</p>
            </div>
            <button
              type="button"
              onClick={openAiRevision}
              disabled={remainingRevisions <= 0}
              className="flex shrink-0 items-center gap-1 rounded-lg bg-violet-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Sparkle size={16} weight="fill" />
              AI 부분 수정
            </button>
          </div>
        )}

        {aiReviseOpen && aiSelection && (
          <div className="rounded-xl border border-violet-200 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <strong className="flex items-center gap-1 text-sm text-slate-900">
                <Sparkle size={17} weight="fill" className="text-violet-600" />
                AI 부분 수정
              </strong>
              <span className="text-xs font-medium text-violet-700">남은 수정 {remainingRevisions} / {MAX_AI_REVISIONS}</span>
            </div>
            <div className="mt-3 rounded-lg bg-slate-50 p-3">
              <span className="text-xs font-medium text-slate-500">선택한 문장</span>
              <p className="mt-1 break-words text-sm text-slate-700">{aiSelection.text}</p>
            </div>
            <div className="mb-3 mt-3 flex flex-wrap gap-2">
              <button type="button" onClick={() => setAiInstruction("더 자연스럽고 읽기 쉽게 고쳐줘")} className="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-700 transition hover:bg-slate-50">자연스럽게</button>
              <button type="button" onClick={() => setAiInstruction("상황을 이해하기 쉽도록 조금 더 구체적으로 써줘")} className="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-700 transition hover:bg-slate-50">더 자세하게</button>
              <button type="button" onClick={() => setAiInstruction("핵심만 남겨서 간결하게 고쳐줘")} className="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-700 transition hover:bg-slate-50">간결하게</button>
            </div>
            <textarea
              value={aiInstruction}
              onChange={(event) => setAiInstruction(event.target.value)}
              rows={3}
              maxLength={500}
              placeholder="예: 증상이 좀 더 잘 드러나도록 자연스럽게 써줘"
              disabled={aiRevisionBusy}
              className="w-full resize-none rounded-xl border border-slate-200 p-3 text-sm text-slate-800 outline-none transition focus:border-violet-500 disabled:bg-slate-50"
            />
            <div className="mt-3 flex justify-end gap-2">
              <button type="button" onClick={closeAiRevision} disabled={aiRevisionBusy} className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:opacity-50">취소</button>
              <button
                type="button"
                onClick={reviseSelectedText}
                disabled={aiRevisionBusy || !aiInstruction.trim() || remainingRevisions <= 0}
                className="flex items-center gap-1 rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Sparkle size={16} weight="fill" />
                {aiRevisionBusy ? "수정 중..." : "수정하기"}
              </button>
            </div>
          </div>
        )}

        {/* 파일 첨부 영역 */}
        <div className="form-field">
          <span>사진 첨부 (최대 5장)</span>

          {isEdit && existingImages.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-2">
              {existingImages.map((image) => (
                <div key={image.id} className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg border border-slate-200">
                  <img
                    src={imageSrc(image.imageUrl)}
                    alt="등록된 사진"
                    className="h-full w-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => removeExistingImage(image)}
                    disabled={removingImageId === image.id}
                    aria-label="사진 삭제"
                    className="absolute -right-1 -top-1 rounded-full bg-white p-0.5 text-red-500 shadow disabled:opacity-50"
                  >
                    <X size={12} weight="bold" />
                  </button>
                </div>
              ))}
            </div>
          )}

          <label className="flex items-center justify-center border-2 border-dashed border-slate-200 rounded-xl p-4 cursor-pointer hover:border-blue-500 transition">
            <Upload size={20} className="mr-2 text-slate-500" />
            <span className="text-sm text-slate-600">이미지 파일 업로드</span>
            <input type="file" multiple accept="image/*" onChange={handleFileChange} className="hidden" />
          </label>
          
          {/* 선택된 파일 목록 프리뷰 */}
          <div className="flex flex-wrap gap-2 mt-2">
            {selectedFiles.map(({ file, previewUrl }, idx) => (
              <div key={idx} className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg border border-slate-200">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={previewUrl} alt={file.name} className="h-full w-full object-cover" />
                <button
                  type="button"
                  onClick={() => removeNewFile(idx)}
                  aria-label="사진 삭제"
                  className="absolute -right-1 -top-1 rounded-full bg-white p-0.5 text-red-500 shadow"
                >
                  <X size={12} weight="bold" />
                </button>
              </div>
            ))}
          </div>
        </div>

        <PostAiAssist files={selectedFiles.map(({ file }) => file)} values={{ title, content, category: selectedCategory }} categories={categories}
          onApply={(field, value) => { if (field === "title") setTitle(value); else if (field === "content") setContent(value); else if (field === "category") setSelectedCategory(value); }}
          onDraftCreated={({ draftId, remainingRevisions: remaining }) => {
            setAiDraftId(draftId);
            setRemainingRevisions(Math.max(0, Math.min(MAX_AI_REVISIONS, remaining)));
          }} />

        {/* 💡 에러 메시지를 파란색 등록 버튼 바로 위로 이동 */}
        {message && (
          <div className="p-3 rounded-xl bg-red-50 text-red-600 text-sm font-medium text-center">
            {message}
          </div>
        )}

        <button type="submit" disabled={submitting} className="w-full bg-blue-600 text-white py-3 rounded-xl font-medium hover:bg-blue-700 transition disabled:opacity-50">
          {submitting ? "저장 중..." : (isEdit ? "수정하기" : "등록하기")}
        </button>
      </form>
    </section>
  );
}