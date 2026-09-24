"use client";

import {
  forwardRef,
  useEffect,
  useImperativeHandle,
} from "react";

import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Underline from "@tiptap/extension-underline";
import "./rich-text-editor.css";


const RichTextEditor = forwardRef(function RichTextEditor(
  {
    value = "",
    onChange,
    onSelectionChange,
  },
  ref,
) {
  const editor = useEditor({
    extensions: [
      StarterKit,
      Underline,
    ],

    content: value || "",

    immediatelyRender: false,

    editorProps: {
      attributes: {
        class:
          "rich-text-editor-content",
      },
    },

    onUpdate({ editor }) {
      const html = editor.getHTML();

      onChange?.(html);
    },

    onSelectionUpdate({ editor }) {
      const { from, to } = editor.state.selection;

      /*
       * 커서만 있고 실제 텍스트 선택이 없으면
       * AI selection 제거.
       */
      if (from === to) {
        onSelectionChange?.(null);
        return;
      }

      const docSize = editor.state.doc.content.size;

      const text = editor.state.doc.textBetween(
        from,
        to,
        " ",
      );

      /*
       * 공백만 선택한 경우 무시
       */
      if (!text.trim()) {
        onSelectionChange?.(null);
        return;
      }

      /*
       * AI가 선택 문장만 보면 문맥을 잘못 이해할 수 있으므로
       * 앞뒤 약 250 position 정도를 함께 전달.
       *
       * TipTap position은 정확히 문자 수와 동일하지는 않지만
       * 이 용도로는 충분하다.
       */
      const contextBefore = editor.state.doc.textBetween(
        Math.max(0, from - 250),
        from,
        " ",
      );

      const contextAfter = editor.state.doc.textBetween(
        to,
        Math.min(docSize, to + 250),
        " ",
      );

      onSelectionChange?.({
        from,
        to,
        text,
        contextBefore,
        contextAfter,
      });
    },
  });

  /*
   * 부모의 value가 외부에서 변경되는 경우 동기화.
   *
   * 대표적인 경우:
   * AI 초안 생성 후
   * PostAiAssist -> setContent(newContent)
   *
   * 이때 editor 안의 내용도 새 값으로 변경해야 한다.
   */
  useEffect(() => {
    if (!editor) return;

    const nextValue = value || "";
    const currentValue = editor.getHTML();

    if (currentValue === nextValue) {
      return;
    }

    /*
     * emitUpdate=false
     *
     * setContent 때문에 onUpdate -> setContent ->
     * useEffect가 반복되는 것을 방지.
     */
    editor.commands.setContent(
      nextValue,
      {
        emitUpdate: false,
      },
    );
  }, [editor, value]);

  /*
   * PostForm에서 editorRef.current.xxx 형태로
   * 사용할 메서드 노출.
   */
  useImperativeHandle(
    ref,
    () => ({
      /*
       * AI가 반환한 replacement로
       * 저장해둔 selection 위치를 교체한다.
       *
       * 현재 editor selection을 사용하지 않고,
       * PostForm이 저장해 둔 from/to를 사용한다.
       *
       * AI 입력창 클릭 시 editor focus가 풀려도
       * 원래 선택 위치를 교체할 수 있기 때문.
       */
      replaceSelection(
        replacement,
        selection,
      ) {
        if (!editor) {
          return false;
        }

        if (
          !replacement ||
          !selection ||
          typeof selection.from !== "number" ||
          typeof selection.to !== "number"
        ) {
          return false;
        }

        const { from, to } = selection;

        const maxPosition =
          editor.state.doc.content.size;

        /*
         * selection 범위 검증.
         *
         * AI 응답을 기다리는 동안 사용자가 본문을 수정하면
         * 기존 from/to가 잘못된 위치를 가리킬 수 있다.
         */
        if (
          from < 0 ||
          to <= from ||
          from > maxPosition ||
          to > maxPosition
        ) {
          return false;
        }

        /*
         * 현재 같은 위치의 텍스트가
         * 사용자가 처음 선택한 텍스트와 같은지 검사.
         *
         * AI 요청 중 사용자가 해당 문장을 수정한 경우
         * 엉뚱한 내용을 덮어쓰는 것을 방지한다.
         */
        const currentSelectedText =
          editor.state.doc.textBetween(
            from,
            to,
            " ",
          );

        if (
          selection.text &&
          currentSelectedText.trim() !==
            selection.text.trim()
        ) {
          return false;
        }

        editor
          .chain()
          .focus()
          .insertContentAt(
            {
              from,
              to,
            },
            replacement,
          )
          .run();

        return true;
      },

      /*
       * 필요할 때 PostForm 등 외부에서
       * 현재 HTML을 얻을 수 있게 제공.
       */
      getHTML() {
        if (!editor) return "";

        return editor.getHTML();
      },

      /*
       * plain text가 필요할 때 사용.
       *
       * AI에게 HTML 대신 일반 텍스트를 보내고 싶을 때
       * 활용 가능.
       */
      getText() {
        if (!editor) return "";

        return editor.getText({
          blockSeparator: "\n",
        });
      },

      /*
       * 에디터 focus
       */
      focus() {
        editor?.chain().focus().run();
      },

      /*
       * 에디터 내용 전체 변경.
       */
      setContent(content) {
        if (!editor) return;

        editor.commands.setContent(
          content || "",
        );
      },

      /*
       * TipTap 기준 실질적으로 내용이 비어있는지 확인.
       */
      isEmpty() {
        if (!editor) return true;

        return editor.isEmpty;
      },
    }),
    [editor],
  );

  if (!editor) {
    return null;
  }

  return (
    <div className="w-full">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-1 rounded-t-xl border border-slate-200 bg-slate-50 p-2">
        {/* Bold */}
        <ToolbarButton
          active={editor.isActive("bold")}
          onClick={() =>
            editor
              .chain()
              .focus()
              .toggleBold()
              .run()
          }
          title="굵게"
        >
          <strong>B</strong>
        </ToolbarButton>

        {/* Italic */}
        <ToolbarButton
          active={editor.isActive("italic")}
          onClick={() =>
            editor
              .chain()
              .focus()
              .toggleItalic()
              .run()
          }
          title="기울임"
        >
          <em>I</em>
        </ToolbarButton>

        {/* Underline */}
        <ToolbarButton
          active={editor.isActive("underline")}
          onClick={() =>
            editor
              .chain()
              .focus()
              .toggleUnderline()
              .run()
          }
          title="밑줄"
        >
          <span className="underline">
            U
          </span>
        </ToolbarButton>

        <Divider />

        {/* Heading 2 */}
        <ToolbarButton
          active={editor.isActive(
            "heading",
            {
              level: 2,
            },
          )}
          onClick={() =>
            editor
              .chain()
              .focus()
              .toggleHeading({
                level: 2,
              })
              .run()
          }
          title="큰 제목"
        >
          H2
        </ToolbarButton>

        {/* Heading 3 */}
        <ToolbarButton
          active={editor.isActive(
            "heading",
            {
              level: 3,
            },
          )}
          onClick={() =>
            editor
              .chain()
              .focus()
              .toggleHeading({
                level: 3,
              })
              .run()
          }
          title="작은 제목"
        >
          H3
        </ToolbarButton>

        <Divider />

        {/* Bullet List */}
        <ToolbarButton
          active={editor.isActive(
            "bulletList",
          )}
          onClick={() =>
            editor
              .chain()
              .focus()
              .toggleBulletList()
              .run()
          }
          title="글머리 목록"
        >
          • 목록
        </ToolbarButton>

        {/* Ordered List */}
        <ToolbarButton
          active={editor.isActive(
            "orderedList",
          )}
          onClick={() =>
            editor
              .chain()
              .focus()
              .toggleOrderedList()
              .run()
          }
          title="번호 목록"
        >
          1. 목록
        </ToolbarButton>

        {/* Blockquote */}
        <ToolbarButton
          active={editor.isActive(
            "blockquote",
          )}
          onClick={() =>
            editor
              .chain()
              .focus()
              .toggleBlockquote()
              .run()
          }
          title="인용"
        >
          “ 인용
        </ToolbarButton>

        <Divider />

        {/* Undo */}
        <ToolbarButton
          disabled={
            !editor.can().chain().focus().undo().run()
          }
          onClick={() =>
            editor
              .chain()
              .focus()
              .undo()
              .run()
          }
          title="실행 취소"
        >
          ↶
        </ToolbarButton>

        {/* Redo */}
        <ToolbarButton
          disabled={
            !editor.can().chain().focus().redo().run()
          }
          onClick={() =>
            editor
              .chain()
              .focus()
              .redo()
              .run()
          }
          title="다시 실행"
        >
          ↷
        </ToolbarButton>
      </div>

      {/* Editor */}
      <EditorContent
        editor={editor}
      />
    </div>
  );
});

export default RichTextEditor;

/*
 * Toolbar 공통 버튼
 */
function ToolbarButton({
  children,
  active = false,
  disabled = false,
  onClick,
  title,
}) {
  return (
    <button
      type="button"
      title={title}
      disabled={disabled}
      onMouseDown={(event) => {
        /*
         * toolbar 버튼 클릭으로 editor selection이
         * 풀리는 것을 방지.
         */
        event.preventDefault();
      }}
      onClick={onClick}
      className={[
        "rounded-md px-2.5 py-1.5 text-xs font-medium transition",
        active
          ? "bg-blue-600 text-white"
          : "text-slate-700 hover:bg-slate-200",
        disabled
          ? "cursor-not-allowed opacity-30"
          : "",
      ].join(" ")}
    >
      {children}
    </button>
  );
}

function Divider() {
  return (
    <span className="mx-1 h-5 w-px bg-slate-300" />
  );
}