"use client";

import { useEffect } from "react";
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
export default function RichTextEditor({ value, onChange, placeholder }) {
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
}
