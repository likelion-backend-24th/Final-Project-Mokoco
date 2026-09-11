"use client";
import { useEffect, useRef, useState } from "react";
import { Plus, Image as ImageIcon, VideoCamera, X } from "@phosphor-icons/react";

function Preview({ file }) {
  const element = useRef(null);
  useEffect(() => {
    const url = URL.createObjectURL(file);
    element.current.src = url;
    return () => URL.revokeObjectURL(url);
  }, [file]);
  return file.type.startsWith("image/")
    // Native images support authenticated media URLs and local object URLs.
    // eslint-disable-next-line @next/next/no-img-element
    ? <img ref={element} alt="전송할 이미지 미리보기" className="max-h-40 rounded-lg" />
    : <video ref={element} controls className="max-h-40 rounded-lg" />;
}

export default function ChatAttachment({ roomId, onSent }) {
  const [file, setFile] = useState(null);
  const [progress, setProgress] = useState(null);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(false);
  const imageInput = useRef(null);
  const videoInput = useRef(null);
  const xhrRef = useRef(null);
  useEffect(() => () => xhrRef.current?.abort(), []);
  const busy = progress !== null;
  function select(event) {
    const value = event.target.files?.[0]; event.target.value = "";
    if (!value) return;
    const image = ["image/jpeg", "image/png", "image/gif", "image/webp"].includes(value.type);
    if (!image && !["video/mp4", "video/webm"].includes(value.type)) { setError("JPG, PNG, GIF, WebP, MP4, WebM만 지원합니다."); return; }
    if (!value.size || value.size > (image ? 10 : 50) * 1024 * 1024) { setError("이미지는 10MB, 동영상은 50MB까지 가능합니다."); return; }
    setError(""); setFile(value); setOpen(false);
  }
  function send() {
    if (!file || xhrRef.current) return;
    setProgress(0); setError("");
    const xhr = new XMLHttpRequest(); xhrRef.current = xhr;
    xhr.open("POST", `/api/chat-rooms/${roomId}/attachments`); xhr.timeout = 150000;
    // 업로드는 post-service 직결(BFF 우회)이라 토큰을 직접 실어 보낸다.
    const token = typeof window !== "undefined" ? localStorage.getItem("access_token") : null;
    if (token) xhr.setRequestHeader("Authorization", `Bearer ${token}`);
    xhr.upload.onprogress = event => { if (event.lengthComputable) setProgress(Math.round(event.loaded / event.total * 100)); };
    xhr.onload = () => {
      try {
        const data = JSON.parse(xhr.responseText);
        if (xhr.status < 200 || xhr.status >= 300) throw new Error(data.error || "첨부 전송에 실패했습니다.");
        onSent(data); setFile(null);
      } catch (failure) { setError(failure.message); }
      setProgress(null); xhrRef.current = null;
    };
    xhr.onerror = xhr.ontimeout = () => { setError("전송 결과를 확인하지 못했습니다. 내역 확인 후 다시 시도해주세요."); setProgress(null); xhrRef.current = null; };
    const data = new FormData(); data.set("file", file); xhr.send(data);
  }
  return <div className="chat-attachment-control">
    <div className="flex items-center gap-2" onKeyDown={event => { if (event.key === "Escape") setOpen(false); }}>
      <button type="button" aria-label={open ? "첨부 메뉴 닫기" : "첨부 메뉴 열기"} aria-expanded={open} disabled={busy} onClick={() => setOpen(value => !value)} className="rounded-full p-2 text-slate-600 hover:bg-slate-200 disabled:opacity-50">{open ? <X size={24} /> : <Plus size={24} />}</button>
      {open && <div className="chat-attachment-menu" role="group" aria-label="첨부 종류">
        <button type="button" aria-label="이미지 첨부" title="이미지 첨부" onClick={() => imageInput.current.click()} className="rounded-xl bg-blue-50 p-3 text-blue-600"><ImageIcon size={24} /></button>
        <button type="button" aria-label="동영상 첨부" title="동영상 첨부" onClick={() => videoInput.current.click()} className="rounded-xl bg-blue-50 p-3 text-blue-600"><VideoCamera size={24} /></button>
      </div>}
      <input ref={imageInput} type="file" aria-label="이미지 선택" accept="image/jpeg,image/png,image/gif,image/webp" onChange={select} className="hidden" />
      <input ref={videoInput} type="file" aria-label="동영상 선택" accept="video/mp4,video/webm" onChange={select} className="hidden" />
    </div>
    {file && <div className="chat-attachment-preview">
      <Preview file={file} />
      <p className="my-2 text-sm text-slate-500">{(file.size / 1024 / 1024).toFixed(1)}MB</p>
      <button disabled={busy} onClick={send} className="mr-3 rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-50">{busy ? progress === 100 ? "저장 중…" : `업로드 ${progress}%` : "첨부 전송"}</button>
      <button disabled={busy} onClick={() => setFile(null)} className="text-sm text-slate-500">취소</button>
    </div>}
    {error && <p role="alert" className="chat-attachment-error">{error}</p>}
  </div>;
}
