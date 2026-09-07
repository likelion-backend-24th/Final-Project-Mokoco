"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Wrench, Upload, X } from "@phosphor-icons/react";
import Link from "next/link";
import { backendUrl } from "@/lib/backend";

const categories = [
  { value: "ELECTRIC_LIGHT", label: "전기·조명" },
  { value: "PLUMBING", label: "배관·설비" },
  { value: "FURNITURE_INSTALL", label: "가구·설치" },
  { value: "HOME_APP_LIANCE", label: "가전제품" },
  { value: "DOOR_WINDOW", label: "문·창문" },
  { value: "LIVING_ETC", label: "생활·기타" },
];

export default function PostForm({ postId, initialValue, userEmail, accessToken }) {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [selectedFiles, setSelectedFiles] = useState([]);
  
  // 기존에 이미 등록되어 있던 이미지 목록 상태 관리
  const [existingImages, setExistingImages] = useState(initialValue?.images || []);
  const [selectedCategory, setSelectedCategory] = useState(initialValue?.category || "ELECTRIC_LIGHT");
  const isEdit = Boolean(postId);

  const handleFileChange = (e) => {
    if (!e.target.files) return;
    const filesArray = Array.from(e.target.files);
    
    if (existingImages.length + selectedFiles.length + filesArray.length > 5) {
      setMessage("이미지는 최대 5장까지 등록할 수 있습니다.");
      return;
    }
    
    setSelectedFiles((prev) => [...prev, ...filesArray]);
    setMessage("");
  };

  const removeNewFile = (index) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const removeExistingImage = (index) => {
    setExistingImages((prev) => prev.filter((_, i) => i !== index));
  };

  async function submitPost(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");

    const formElement = event.currentTarget;
    const title = formElement.elements.namedItem("title").value;
    const content = formElement.elements.namedItem("content").value;

    const formData = new FormData();

    // 수정 시 기존 이미지 중 유지할 목록(existingImages)도 함께 백엔드로 전달해야 할 수 있습니다.
    const postDto = { 
      title, 
      content, 
      category: selectedCategory,
      images: existingImages // 백엔드 DTO 구조에 맞춰 유지할 이미지 전송
    };

    formData.append(
      "post",
      new Blob([JSON.stringify(postDto)], { type: "application/json" })
    );

    selectedFiles.forEach((file) => {
      formData.append("images", file);
    });

    try {
      const response = await fetch(isEdit ? backendUrl(`/posts/${postId}`) : backendUrl("/posts"), {
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
              {/* 기존에 업로드되어 있던 이미지 미리보기 및 삭제 */}
              {existingImages.map((img, idx) => {
                const imgUrl = typeof img === "string" ? img : img.imageUrl;
                return (
                  <div key={`existing-${idx}`} className="file-preview-item flex items-center gap-2">
                    <img src={backendUrl(imgUrl)} alt="기존 이미지" className="w-8 h-8 object-cover rounded" />
                    <span className="truncate max-w-[120px]">기존 이미지 {idx + 1}</span>
                    <button type="button" onClick={() => removeExistingImage(idx)}><X size={14} /></button>
                  </div>
                );
              })}

              {/* 새로 추가한 파일 미리보기 및 삭제 */}
              {selectedFiles.map((file, idx) => (
                <div key={`new-${idx}`} className="file-preview-item flex items-center gap-2">
                  <span className="truncate max-w-[120px]">{file.name}</span>
                  <button type="button" onClick={() => removeNewFile(idx)}><X size={14} /></button>
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