"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  Wrench,
  Upload,
  X,
  Sparkle,
} from "@phosphor-icons/react";
import Link from "next/link";

import { backendUrl, imageSrc } from "@/lib/backend";
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

export default function PostForm({
  postId,
  initialValue,
  accessToken,
}) {
  const router = useRouter();

  const editorRef = useRef(null);

  /*
   * 게시글 기본 상태
   */
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");

  const [title, setTitle] = useState(initialValue?.title || "");
  const [content, setContent] = useState(initialValue?.content || "");
  const [selectedCategory, setSelectedCategory] = useState(
    initialValue?.category || "ELECTRIC_LIGHT",
  );

  /*
   * 이미지 상태
   */
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [existingImages, setExistingImages] = useState(
    initialValue?.images || [],
  );
  const [removingImageId, setRemovingImageId] = useState(null);

  /*
   * AI 부분 수정용 상태
   *
   * 예상 형태:
   *
   * {
   *   from: 10,
   *   to: 25,
   *   text: "...",
   *   contextBefore: "...",
   *   contextAfter: "..."
   * }
   */
  const [aiSelection, setAiSelection] = useState(null);

  /*
   * AI 부분 수정 UI 상태
   */
  const [aiReviseOpen, setAiReviseOpen] = useState(false);
  const [aiInstruction, setAiInstruction] = useState("");
  const [aiRevisionBusy, setAiRevisionBusy] = useState(false);

  /*
   * 현재는 프론트 기본값.
   *
   * 이후 백엔드에서 draftId + remainingRevisions를
   * 반환하면 서버 응답값으로 갱신한다.
   */
  const [remainingRevisions, setRemainingRevisions] =
    useState(MAX_AI_REVISIONS);

  /*
   * AI 초안 세션 ID.
   *
   * 이후 /api/ai/post-draft 응답에서 draftId를 받아
   * PostAiAssist -> PostForm으로 올려준다.
   */
  const [aiDraftId, setAiDraftId] = useState(null);

  const isEdit = Boolean(postId);

  const authHeaders = {
    ...(accessToken
      ? {
        Authorization: `Bearer ${accessToken}`,
      }
      : {}),
  };

  /*
   * 새 이미지 선택
   */
  const handleFileChange = (event) => {
    if (!event.target.files) return;

    const filesArray = Array.from(event.target.files);

    if (
      existingImages.length +
      selectedFiles.length +
      filesArray.length >
      5
    ) {
      setMessage("이미지는 최대 5장까지 등록할 수 있습니다.");
      event.target.value = "";
      return;
    }

    setSelectedFiles((prev) => [...prev, ...filesArray]);
    setMessage("");

    /*
     * 같은 파일을 다시 선택할 수 있도록 초기화
     */
    event.target.value = "";
  };

  /*
   * 새로 선택한 이미지 제거
   */
  const removeNewFile = (index) => {
    setSelectedFiles((prev) =>
      prev.filter((_, fileIndex) => fileIndex !== index),
    );
  };

  /*
   * 기존 이미지 제거
   */
  async function removeExistingImage(image) {
    if (!postId || removingImageId) return;

    setRemovingImageId(image.id);
    setMessage("");

    try {
      const response = await fetch(
        backendUrl(`/api/posts/${postId}/images/${image.id}`),
        {
          method: "DELETE",
          headers: authHeaders,
          credentials: "include",
        },
      );

      if (!response.ok) {
        let errorMessage = "이미지를 삭제하지 못했습니다.";

        try {
          const payload = await response.json();
          errorMessage = payload.message ?? errorMessage;
        } catch {
          // body 없음
        }

        setMessage(errorMessage);
        return;
      }

      setExistingImages((prev) =>
        prev.filter((img) => img.id !== image.id),
      );
    } catch {
      setMessage("이미지를 삭제하지 못했습니다.");
    } finally {
      setRemovingImageId(null);
    }
  }

  /*
   * RichTextEditor selection 변경 처리
   */
  function handleSelectionChange(selection) {
    setAiSelection(selection);

    /*
     * 선택이 해제되었는데 팝업이 열려있지 않은 경우에는
     * 별도 작업 필요 없음.
     *
     * AI 팝업을 띄운 뒤에는 editor focus 이동 때문에
     * selection이 사라질 수 있으므로 팝업이 열려있다면
     * 기존 aiSelection은 RichTextEditor 구현 방식에 따라
     * 유지하도록 조정할 수도 있다.
     */
  }

  /*
   * AI 부분 수정 창 열기
   */
  function openAiRevision() {

    if (!aiDraftId) {
      setMessage("먼저 AI 초안을 생성해주세요.");
      return;
    }

    if (!aiSelection?.text?.trim()) {
      setMessage("AI로 수정할 문장을 먼저 선택해주세요.");
      return;
    }

    if (remainingRevisions <= 0) {
      setMessage("AI 부분 수정 횟수를 모두 사용했습니다.");
      return;
    }

    setMessage("");
    setAiInstruction("");
    setAiReviseOpen(true);
  }

  /*
   * AI 부분 수정 취소
   */
  function closeAiRevision() {
    if (aiRevisionBusy) return;

    setAiReviseOpen(false);
    setAiInstruction("");
  }

  /*
   * AI 부분 수정 요청
   *
   * 백엔드 API:
   * POST /api/ai/post-revise
   *
   * 아직 API 구현 전이라면 이 함수까지 만들어두고
   * 백엔드 연결할 때 사용할 수 있다.
   */
  async function reviseSelectedText() {
    if (aiRevisionBusy) return;

    if (!aiSelection?.text?.trim()) {
      setMessage("AI로 수정할 문장을 선택해주세요.");
      return;
    }

    const instruction = aiInstruction.trim();

    if (!instruction) {
      setMessage("어떻게 수정할지 요청 내용을 입력해주세요.");
      return;
    }

    if (remainingRevisions <= 0) {
      setMessage("AI 부분 수정 횟수를 모두 사용했습니다.");
      return;
    }

    setAiRevisionBusy(true);
    setMessage("");

    try {
      const response = await fetch("/api/ai/post-revise", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
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
        setMessage(
          payload?.message ||
          "AI 부분 수정에 실패했습니다. 직접 수정해주세요.",
        );
        return;
      }

      const replacement = payload?.replacement;

      if (!replacement?.trim()) {
        setMessage(
          "AI가 수정 결과를 만들지 못했습니다. 다시 시도해주세요.",
        );
        return;
      }

      /*
       * RichTextEditor에서
       *
       * useImperativeHandle(ref, () => ({
       *   replaceSelection(...)
       * }))
       *
       * 형태로 제공할 예정.
       */
      const replaced = editorRef.current?.replaceSelection?.(
        replacement,
        aiSelection,
      );

      /*
       * replaceSelection이 false를 반환하도록 구현할 수도 있으므로
       * 명시적으로 실패 처리 가능.
       */
      if (replaced === false) {
        setMessage(
          "선택 영역이 변경되었습니다. 문장을 다시 선택해주세요.",
        );
        return;
      }

      /*
       * 서버가 남은 횟수를 내려주는 것이 최종 구조.
       */
      if (typeof payload.remainingRevisions === "number") {
        setRemainingRevisions(payload.remainingRevisions);
      } else {
        setRemainingRevisions((prev) => Math.max(prev - 1, 0));
      }

      setAiReviseOpen(false);
      setAiInstruction("");
      setAiSelection(null);
    } catch {
      setMessage(
        "AI 서버와 통신할 수 없습니다. 입력 내용은 유지됩니다.",
      );
    } finally {
      setAiRevisionBusy(false);
    }
  }

  /*
   * AI 초안 생성 결과 적용
   */
  function handleAiDraftApply(field, value) {
    if (field === "title") {
      setTitle(value);
      return;
    }

    if (field === "content") {
      setContent(value);
      return;
    }

    if (field === "category") {
      setSelectedCategory(value);
    }
  }

  /*
   * 게시글 등록 / 수정
   */
  async function submitPost(event) {
    event.preventDefault();

    if (submitting) return;

    /*
     * RichTextEditor의 빈 문서는
     *
     * <p></p>
     *
     * 같은 HTML일 수도 있으므로 추후 Editor 내부에서
     * isEmpty 값을 반환받아 검증하는 편이 더 정확하다.
     */
    if (!title.trim()) {
      setMessage("제목을 입력해주세요.");
      return;
    }

    if (editorRef.current?.isEmpty()) {
      setMessage("내용을 입력해주세요.");
      return;
    }

    setSubmitting(true);
    setMessage("");

    const postDto = {
      title: title.trim(),
      content,
      category: selectedCategory,
      contentFormat: "HTML",
    };

    try {
      let response;

      /*
       * 게시글 수정
       */
      if (isEdit) {
        response = await fetch(
          backendUrl(`/api/posts/${postId}`),
          {
            method: "PATCH",
            headers: {
              "Content-Type": "application/json",
              ...authHeaders,
            },
            credentials: "include",
            body: JSON.stringify(postDto),
          },
        );
      } else {
        /*
         * 게시글 신규 등록
         */
        const formData = new FormData();

        formData.append(
          "post",
          new Blob([JSON.stringify(postDto)], {
            type: "application/json",
          }),
        );

        selectedFiles.forEach((file) => {
          formData.append("images", file);
        });

        response = await fetch(backendUrl("/api/posts"), {
          method: "POST",
          headers: authHeaders,
          credentials: "include",
          body: formData,
        });
      }

      if (!response.ok) {
        let errorMessage =
          isEdit
            ? "수리 요청을 수정하지 못했습니다."
            : "수리 요청을 등록하지 못했습니다.";

        try {
          const payload = await response.json();
          errorMessage = payload.message ?? errorMessage;
        } catch {
          // body 없음
        }

        setMessage(errorMessage);
        return;
      }

      /*
       * 수정 화면에서 새로 추가한 이미지는
       * 별도 API로 업로드
       */
      if (isEdit && selectedFiles.length > 0) {
        const imageForm = new FormData();

        selectedFiles.forEach((file) => {
          imageForm.append("images", file);
        });

        const imageResponse = await fetch(
          backendUrl(`/api/posts/${postId}/images`),
          {
            method: "POST",
            headers: authHeaders,
            credentials: "include",
            body: imageForm,
          },
        );

        if (!imageResponse.ok) {
          let errorMessage =
            "글 내용은 수정됐지만 사진 추가에 실패했습니다.";

          try {
            const payload = await imageResponse.json();
            errorMessage = payload.message ?? errorMessage;
          } catch {
            // body 없음
          }

          setMessage(errorMessage);
          return;
        }
      }

      /*
       * PATCH
       * → body 없음
       *
       * POST
       * → 생성된 postId 반환
       */
      const targetId = isEdit
        ? postId
        : await response.json();

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
      <Link
        href="/posts"
        className="repair-form-back"
      >
        <ArrowLeft size={18} />
        목록으로 돌아가기
      </Link>

      <span className="repair-form-icon">
        <Wrench
          size={30}
          weight="duotone"
        />
      </span>

      <h1>
        {isEdit
          ? "수리 요청 수정"
          : "수리 요청하기"}
      </h1>

      <p>
        {isEdit
          ? "내용을 고치고 저장하면 바로 반영돼요."
          : "어떤 도움이 필요한지 이웃이 이해하기 쉽게 알려주세요."}
      </p>

      <form
        onSubmit={submitPost}
        className="repair-form"
      >
        {/* 카테고리 */}
        <label className="form-field">
          <span>카테고리</span>

          <select
            value={selectedCategory}
            onChange={(event) =>
              setSelectedCategory(event.target.value)
            }
            className="w-full rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
          >
            {categories.map((category) => (
              <option
                key={category.value}
                value={category.value}
              >
                {category.label}
              </option>
            ))}
          </select>
        </label>

        {/* 제목 */}
        <label className="form-field">
          <span>제목</span>

          <input
            name="title"
            type="text"
            value={title}
            onChange={(event) =>
              setTitle(event.target.value)
            }
            placeholder="제목을 입력해주세요"
            required
            maxLength={100}
            className="w-full rounded-xl border border-slate-200 p-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
          />
        </label>

        {/* Rich Text 본문 */}
        <div className="form-field">
          <span>내용</span>

          <RichTextEditor
            ref={editorRef}
            value={content}
            onChange={setContent}
            onSelectionChange={handleSelectionChange}
          />

          {/* 텍스트 선택 시 AI 부분 수정 버튼 */}
          {aiSelection?.text?.trim() &&
            !aiReviseOpen && (
              <div className="mt-2 flex items-center justify-between gap-3 rounded-xl border border-violet-200 bg-violet-50 p-3">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-violet-900">
                    선택한 문장을 AI로 수정할 수 있어요.
                  </p>

                  <p className="mt-1 truncate text-xs text-violet-700">
                    “{aiSelection.text}”
                  </p>
                </div>

                <button
                  type="button"
                  onClick={openAiRevision}
                  disabled={
                    remainingRevisions <= 0
                  }
                  className="flex shrink-0 items-center gap-1 rounded-lg bg-violet-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Sparkle
                    size={16}
                    weight="fill"
                  />

                  AI 부분 수정
                </button>
              </div>
            )}

          {/* AI 부분 수정 패널 */}
          {aiReviseOpen && aiSelection && (
            <div className="mt-3 rounded-xl border border-violet-200 bg-white p-4 shadow-sm">
              <div className="mb-3">
                <div className="flex items-center justify-between gap-3">
                  <strong className="flex items-center gap-1 text-sm text-slate-900">
                    <Sparkle
                      size={17}
                      weight="fill"
                      className="text-violet-600"
                    />
                    AI 부분 수정
                  </strong>

                  <span className="text-xs font-medium text-violet-700">
                    남은 수정{" "}
                    {remainingRevisions} /{" "}
                    {MAX_AI_REVISIONS}
                  </span>
                </div>

                <div className="mt-3 rounded-lg bg-slate-50 p-3">
                  <span className="text-xs font-medium text-slate-500">
                    선택한 문장
                  </span>

                  <p className="mt-1 break-words text-sm text-slate-700">
                    {aiSelection.text}
                  </p>
                </div>
              </div>

              {/* 빠른 요청 */}
              <div className="mb-3 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() =>
                    setAiInstruction(
                      "더 자연스럽고 읽기 쉽게 수정해줘",
                    )
                  }
                  className="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-700 transition hover:bg-slate-50"
                >
                  자연스럽게
                </button>

                <button
                  type="button"
                  onClick={() =>
                    setAiInstruction(
                      "상황을 이해하기 쉽도록 조금 더 구체적으로 작성해줘",
                    )
                  }
                  className="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-700 transition hover:bg-slate-50"
                >
                  더 자세하게
                </button>

                <button
                  type="button"
                  onClick={() =>
                    setAiInstruction(
                      "핵심 내용만 남겨 간결하게 수정해줘",
                    )
                  }
                  className="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-700 transition hover:bg-slate-50"
                >
                  간결하게
                </button>
              </div>

              <textarea
                value={aiInstruction}
                onChange={(event) =>
                  setAiInstruction(
                    event.target.value,
                  )
                }
                rows={3}
                maxLength={500}
                placeholder="예: 증상이 좀 더 잘 드러나도록 자연스럽게 써줘"
                disabled={aiRevisionBusy}
                className="w-full resize-none rounded-xl border border-slate-200 p-3 text-sm text-slate-800 outline-none transition focus:border-violet-500 disabled:bg-slate-50"
              />

              <div className="mt-3 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={closeAiRevision}
                  disabled={aiRevisionBusy}
                  className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:opacity-50"
                >
                  취소
                </button>

                <button
                  type="button"
                  onClick={reviseSelectedText}
                  disabled={
                    aiRevisionBusy ||
                    !aiInstruction.trim() ||
                    remainingRevisions <= 0
                  }
                  className="flex items-center gap-1 rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Sparkle
                    size={16}
                    weight="fill"
                  />

                  {aiRevisionBusy
                    ? "수정 중..."
                    : "수정하기"}
                </button>
              </div>
            </div>
          )}

          {remainingRevisions === 0 && (
            <p className="mt-2 text-xs text-slate-500">
              AI 부분 수정 3회를 모두
              사용했습니다. 이후에는 직접 수정할 수
              있습니다.
            </p>
          )}
        </div>

        {/* 사진 첨부 */}
        <div className="form-field">
          <span>사진 첨부 (최대 5장)</span>

          {/* 기존 이미지 */}
          {isEdit &&
            existingImages.length > 0 && (
              <div className="mb-2 flex flex-wrap gap-2">
                {existingImages.map((image) => (
                  <div
                    key={image.id}
                    className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg border border-slate-200"
                  >
                    <img
                      src={imageSrc(
                        image.imageUrl,
                      )}
                      alt="등록된 사진"
                      className="h-full w-full object-cover"
                    />

                    <button
                      type="button"
                      onClick={() =>
                        removeExistingImage(
                          image,
                        )
                      }
                      disabled={
                        removingImageId ===
                        image.id
                      }
                      aria-label="사진 삭제"
                      className="absolute -right-1 -top-1 rounded-full bg-white p-0.5 text-red-500 shadow disabled:opacity-50"
                    >
                      <X
                        size={12}
                        weight="bold"
                      />
                    </button>
                  </div>
                ))}
              </div>
            )}

          {/* 새 이미지 업로드 */}
          <label className="flex cursor-pointer items-center justify-center rounded-xl border-2 border-dashed border-slate-200 p-4 transition hover:border-blue-500">
            <Upload
              size={20}
              className="mr-2 text-slate-500"
            />

            <span className="text-sm text-slate-600">
              이미지 파일 업로드
            </span>

            <input
              type="file"
              multiple
              accept="image/*"
              onChange={handleFileChange}
              className="hidden"
            />
          </label>

          {/* 새로 선택한 이미지 목록 */}
          {selectedFiles.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-2">
              {selectedFiles.map(
                (file, index) => (
                  <div
                    key={`${file.name}-${file.lastModified}-${index}`}
                    className="flex items-center rounded-lg bg-slate-100 px-3 py-1 text-xs"
                  >
                    <span className="max-w-48 truncate">
                      {file.name}
                    </span>

                    <button
                      type="button"
                      onClick={() =>
                        removeNewFile(index)
                      }
                      aria-label={`${file.name} 삭제`}
                      className="ml-2 text-red-500"
                    >
                      <X size={14} />
                    </button>
                  </div>
                ),
              )}
            </div>
          )}
        </div>

        {/* 기존 AI 사진 분석 → 초안 생성 */}
        <PostAiAssist
          files={selectedFiles}
          values={{
            title,
            content,
            category: selectedCategory,
          }}
          categories={categories}
          onApply={handleAiDraftApply}
          onDraftCreated={({ draftId, remainingRevisions }) => {
            setAiDraftId(draftId);
            setRemainingRevisions(
              Math.max(0, Math.min(MAX_AI_REVISIONS, remainingRevisions)),
            );
          }}


        />

        {/* 에러 */}
        {message && (
          <div
            role="alert"
            className="rounded-xl bg-red-50 p-3 text-center text-sm font-medium text-red-600"
          >
            {message}
          </div>
        )}

        {/* 등록 / 수정 */}
        <button
          type="submit"
          disabled={
            submitting || aiRevisionBusy
          }
          className="w-full rounded-xl bg-blue-600 py-3 font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting
            ? "저장 중..."
            : isEdit
              ? "수정하기"
              : "등록하기"}
        </button>
      </form>
    </section>
  );
}