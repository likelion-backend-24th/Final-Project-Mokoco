"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Wrench, Upload, X } from "@phosphor-icons/react";
import Link from "next/link";

export default function PostForm({ postId, initialValue, userEmail, accessToken }) {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [selectedFiles, setSelectedFiles] = useState([]);
  const isEdit = Boolean(postId);

  const handleFileChange = (e) => {
    if (!e.target.files) return;
    const filesArray = Array.from(e.target.files);
    
    if (selectedFiles.length + filesArray.length > 5) {
      setMessage("이미지는 최대 5장까지 등록할 수 있습니다.");
      return;
    }
    
    setSelectedFiles((prev) => [...prev, ...filesArray]);
    setMessage("");
  };

  const removeFile = (index) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  async function submitPost(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");

    const formElement = event.currentTarget;
    const title = formElement.elements.namedItem("title").value;
    const content = formElement.elements.namedItem("content").value;

    const formData = new FormData();

    // 1. @RequestPart("post")에 매핑될 JSON 데이터를 Blob으로 감싸서 추가
    const postDto = { title, content };
    formData.append(
      "post",
      new Blob([JSON.stringify(postDto)], { type: "application/json" })
    );

    // 2. @RequestPart("images")에 매핑될 파일들 추가
    selectedFiles.forEach((file) => {
      formData.append("images", file);
    });

    try {
      const response = await fetch(isEdit ? `/api/posts/${postId}` : "/api/posts", {
        method: isEdit ? "PATCH" : "POST",
        headers: {
          "X-User-Email": userEmail ?? "",
          ...(accessToken ? { "Authorization": `Bearer ${accessToken}` } : {}),
        },
        credentials: "include",
        body: formData,
      });

      const payload = await response.json();

      if (!response.ok) {
        setMessage(payload.message ?? "수리 요청을 등록하지 못했습니다.");
        return;
      }

      router.push(isEdit ? `/posts/${postId}` : `/posts/${payload}`);
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
          <span>제목</span>
          <input name="title" type="text" maxLength={100} required defaultValue={initialValue?.title} placeholder="예: 세면대 수도꼭지에서 물이 새요" />
        </label>
        
        <label className="form-field">
          <span>요청 내용</span>
          <textarea name="content" required rows={7} defaultValue={initialValue?.content} placeholder="문제가 발생한 상황과 필요한 도움을 자세히 적어주세요." />
        </label>

        <label className="form-field">
          <span>사진 첨부 (최대 5장)</span>
          <div className="file-upload-box">
            <input type="file" accept="image/*" multiple onChange={handleFileChange} id="image-input" className="hidden" />
            <label htmlFor="image-input" className="file-upload-button cursor-pointer">
              <Upload size={20} /> 사진 선택하기
            </label>
            <div className="file-preview-list">
              {selectedFiles.map((file, idx) => (
                <div key={idx} className="file-preview-item">
                  <span>{file.name}</span>
                  <button type="button" onClick={() => removeFile(idx)}><X size={14} /></button>
                </div>
              ))}
            </div>
          </div>
        </label>

        {message && <div className="form-message form-message-error" role="alert">{message}</div>}

        <button type="submit" className="primary-button w-full justify-center" disabled={submitting}>
          {submitting ? "저장 중..." : isEdit ? "수정 완료" : "수리 요청 등록"}
        </button>
      </form>
    </section>
  );
}