"use client";

import { useEffect } from "react";
import { EditorContent, useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Underline from "@tiptap/extension-underline";
import Link from "@tiptap/extension-link";
import "./rich-text.css";

function ToolButton({ active = false, disabled = false, label, onClick, children }) {
  return (
    <button type="button" className={active ? "is-active" : ""} disabled={disabled}
      aria-pressed={active} aria-label={label} title={label} onClick={onClick}>
      {children}
    </button>
  );
}

export default function PostRichTextEditor({ value, onChange }) {
  const editor = useEditor({
    immediatelyRender: false,
    extensions: [
      StarterKit.configure({ heading: { levels: [2, 3] }, link: false, underline: false }),
      Underline,
      Link.configure({ openOnClick: false, autolink: false, protocols: ["http", "https"],
        HTMLAttributes: { rel: "nofollow noopener noreferrer", target: "_blank" } }),
    ],
    content: value,
    editorProps: { attributes: { class: "rich-text-editor-content", "aria-label": "게시글 내용" } },
    onCreate: ({ editor: current }) => onChange(current.getHTML(), current.getText()),
    onUpdate: ({ editor: current }) => onChange(current.getHTML(), current.getText()),
  });

  useEffect(() => {
    if (editor && value !== editor.getHTML()) editor.commands.setContent(value, { emitUpdate: false });
  }, [editor, value]);

  if (!editor) return <div className="rich-text-editor-loading">편집기를 준비하고 있습니다.</div>;

  function editLink() {
    const previous = editor.getAttributes("link").href || "https://";
    const href = window.prompt("연결할 주소를 입력해주세요.", previous);
    if (href === null) return;
    if (!href.trim()) {
      editor.chain().focus().unsetLink().run();
      return;
    }
    try {
      const url = new URL(href);
      if (!['http:', 'https:'].includes(url.protocol)) throw new Error();
      editor.chain().focus().extendMarkRange("link").setLink({ href: url.toString() }).run();
    } catch {
      window.alert("http 또는 https 주소만 사용할 수 있습니다.");
    }
  }

  return (
    <div className="rich-text-editor">
      <div className="rich-text-toolbar" role="toolbar" aria-label="본문 서식">
        <ToolButton label="굵게" active={editor.isActive("bold")} onClick={() => editor.chain().focus().toggleBold().run()}>B</ToolButton>
        <ToolButton label="기울임" active={editor.isActive("italic")} onClick={() => editor.chain().focus().toggleItalic().run()}><i>I</i></ToolButton>
        <ToolButton label="밑줄" active={editor.isActive("underline")} onClick={() => editor.chain().focus().toggleUnderline().run()}><u>U</u></ToolButton>
        <ToolButton label="제목 2" active={editor.isActive("heading", { level: 2 })} onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}>H2</ToolButton>
        <ToolButton label="제목 3" active={editor.isActive("heading", { level: 3 })} onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}>H3</ToolButton>
        <ToolButton label="글머리 목록" active={editor.isActive("bulletList")} onClick={() => editor.chain().focus().toggleBulletList().run()}>• 목록</ToolButton>
        <ToolButton label="번호 목록" active={editor.isActive("orderedList")} onClick={() => editor.chain().focus().toggleOrderedList().run()}>1. 목록</ToolButton>
        <ToolButton label="인용" active={editor.isActive("blockquote")} onClick={() => editor.chain().focus().toggleBlockquote().run()}>인용</ToolButton>
        <ToolButton label="링크" active={editor.isActive("link")} onClick={editLink}>링크</ToolButton>
        <ToolButton label="실행 취소" disabled={!editor.can().undo()} onClick={() => editor.chain().focus().undo().run()}>↶</ToolButton>
        <ToolButton label="다시 실행" disabled={!editor.can().redo()} onClick={() => editor.chain().focus().redo().run()}>↷</ToolButton>
      </div>
      <EditorContent editor={editor} />
    </div>
  );
}
