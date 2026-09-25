"use client";

import { forwardRef, useEffect, useImperativeHandle } from "react";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Underline from "@tiptap/extension-underline";
import Placeholder from "@tiptap/extension-placeholder";
import {
  TextB, TextItalic, TextUnderline, TextStrikethrough,
  TextHTwo, TextHThree, ListBullets, ListNumbers, Quotes,
  ArrowCounterClockwise, ArrowClockwise,
} from "@phosphor-icons/react";
import "./rich-text-editor.css";

function ToolbarButton({ onClick, active, disabled, label, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      aria-pressed={active}
      className={`rte-btn ${active ? "is-active" : ""}`}
    >
      {children}
    </button>
  );
}

// 저장 시 서버가 허용하는 태그(p/br/strong/em/u/s/h2/h3/ul/ol/li/blockquote)만 생기도록
// StarterKit에서 쓰지 않는 노드(코드블록/구분선/인라인코드)는 아예 꺼둔다 — 에디터에서
// 보이는 서식이 저장 후에도 그대로 유지되게(정제 과정에서 조용히 사라지지 않게) 하기 위함.
const RichTextEditor = forwardRef(function RichTextEditor({ value, onChange, placeholder, onSelectionChange }, ref) {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [2, 3] },
        codeBlock: false,
        code: false,
        horizontalRule: false,
      }),
      Underline,
      Placeholder.configure({ placeholder: placeholder || "내용을 입력해주세요" }),
    ],
    content: value || "",
    immediatelyRender: false,
    onUpdate: ({ editor }) => onChange(editor.getHTML()),
    // 커서만 있고 실제 선택 범위가 없으면(from===to) AI 부분 수정 대상이 없는 것이므로 null을
    // 올려서 상위(PostForm)가 제안 배너를 접게 한다. AI에게 문맥을 같이 주기 위해 선택 앞뒤
    // 약 250자를 함께 담아 올린다(TipTap position은 글자 수와 정확히 같지는 않지만 이 용도엔 충분).
    onSelectionUpdate: ({ editor }) => {
      if (!onSelectionChange) return;
      const { from, to } = editor.state.selection;
      if (from === to) { onSelectionChange(null); return; }
      const text = editor.state.doc.textBetween(from, to, " ");
      if (!text.trim()) { onSelectionChange(null); return; }
      const docSize = editor.state.doc.content.size;
      onSelectionChange({
        from, to, text,
        contextBefore: editor.state.doc.textBetween(Math.max(0, from - 250), from, " "),
        contextAfter: editor.state.doc.textBetween(to, Math.min(docSize, to + 250), " "),
      });
    },
    editorProps: {
      attributes: {
        class: "rte-content",
        "aria-label": placeholder || "내용",
      },
    },
  });

  // AI 초안 적용처럼 에디터 바깥에서 content 상태가 바뀔 때만 동기화한다(내가 타이핑한
  // 결과를 onUpdate로 이미 반영했는데 또 setContent하면 커서가 튀므로, 값이 다를 때만).
  useEffect(() => {
    if (!editor) return;
    if (value !== undefined && value !== editor.getHTML()) {
      editor.commands.setContent(value || "", false);
    }
  }, [value, editor]);

  useImperativeHandle(ref, () => ({
    // AI가 반환한 replacement로 선택했던 범위(from/to)를 교체한다. 문자열을 그대로 넘기면
    // TipTap이 HTML로 파싱하므로, AI 응답이 실수로라도 마크업을 포함하면 안 되니 순수 텍스트
    // 노드로 명시해서 넣는다(HTML로 해석될 여지 자체를 없앤다).
    replaceSelection(replacement, selection) {
      if (!editor || !replacement || !selection) return false;
      const { from, to } = selection;
      const maxPosition = editor.state.doc.content.size;
      if (from < 0 || to <= from || from > maxPosition || to > maxPosition) return false;
      // AI 응답을 기다리는 동안 사용자가 그 문장을 수정했다면, 엉뚱한 위치를 덮어쓰지 않도록 취소한다.
      const currentSelectedText = editor.state.doc.textBetween(from, to, " ");
      if (selection.text && currentSelectedText.trim() !== selection.text.trim()) return false;
      editor.chain().focus().insertContentAt({ from, to }, { type: "text", text: replacement }).run();
      return true;
    },
  }), [editor]);

  if (!editor) return null;

  return (
    <div className="rich-text-editor">
      <div className="rte-toolbar" role="toolbar" aria-label="서식 도구">
        <ToolbarButton label="굵게" active={editor.isActive("bold")} onClick={() => editor.chain().focus().toggleBold().run()}><TextB size={16} weight="bold" /></ToolbarButton>
        <ToolbarButton label="기울임" active={editor.isActive("italic")} onClick={() => editor.chain().focus().toggleItalic().run()}><TextItalic size={16} weight="bold" /></ToolbarButton>
        <ToolbarButton label="밑줄" active={editor.isActive("underline")} onClick={() => editor.chain().focus().toggleUnderline().run()}><TextUnderline size={16} weight="bold" /></ToolbarButton>
        <ToolbarButton label="취소선" active={editor.isActive("strike")} onClick={() => editor.chain().focus().toggleStrike().run()}><TextStrikethrough size={16} weight="bold" /></ToolbarButton>
        <span className="rte-divider" aria-hidden="true" />
        <ToolbarButton label="제목 2" active={editor.isActive("heading", { level: 2 })} onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}><TextHTwo size={16} weight="bold" /></ToolbarButton>
        <ToolbarButton label="제목 3" active={editor.isActive("heading", { level: 3 })} onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}><TextHThree size={16} weight="bold" /></ToolbarButton>
        <span className="rte-divider" aria-hidden="true" />
        <ToolbarButton label="글머리 목록" active={editor.isActive("bulletList")} onClick={() => editor.chain().focus().toggleBulletList().run()}><ListBullets size={16} weight="bold" /></ToolbarButton>
        <ToolbarButton label="번호 목록" active={editor.isActive("orderedList")} onClick={() => editor.chain().focus().toggleOrderedList().run()}><ListNumbers size={16} weight="bold" /></ToolbarButton>
        <ToolbarButton label="인용구" active={editor.isActive("blockquote")} onClick={() => editor.chain().focus().toggleBlockquote().run()}><Quotes size={16} weight="bold" /></ToolbarButton>
        <span className="rte-divider" aria-hidden="true" />
        <ToolbarButton label="실행 취소" disabled={!editor.can().undo()} onClick={() => editor.chain().focus().undo().run()}><ArrowCounterClockwise size={16} weight="bold" /></ToolbarButton>
        <ToolbarButton label="다시 실행" disabled={!editor.can().redo()} onClick={() => editor.chain().focus().redo().run()}><ArrowClockwise size={16} weight="bold" /></ToolbarButton>
      </div>
      <EditorContent editor={editor} />
    </div>
  );
});

export default RichTextEditor;
