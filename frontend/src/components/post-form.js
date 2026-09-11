"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Wrench, Upload, X } from "@phosphor-icons/react";
import Link from "next/link";
import { backendUrl, imageSrc } from "@/lib/backend";

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
  
  const [existingImages, setExistingImages] = useState(initialValue?.images || []);
  const [removingImageId, setRemovingImageId] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState(initialValue?.category || "ELECTRIC_LIGHT");
  const isEdit = Boolean(postId);

  const authHeaders = {
    "X-User-Email": userEmail ?? "",
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
  };

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

  async function submitPost(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");

    const formElement = event.currentTarget;
    const title = formElement.elements.namedItem("title").value;
    const content = formElement.elements.namedItem("content").value;
    const postDto = { title, content, category: selectedCategory };

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
        selectedFiles.forEach((file) => formData.append("images", file));
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
        selectedFiles.forEach((file) => imgForm.append("images", file));
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
            defaultValue={initialValue?.title || ""} 
            placeholder="제목을 입력해주세요" 
            required
            className="w-full rounded-xl border border-slate-200 p-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
          />
        </label>

        {/* 내용 입력 */}
        <label className="form-field">
          <span>내용</span>
          <textarea 
            name="content" 
            rows={6}
            defaultValue={initialValue?.content || ""} 
            placeholder="어떤 도움이 필요한지 자세히 적어주세요" 
            required
            className="w-full rounded-xl border border-slate-200 p-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
          />
        </label>

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
            {selectedFiles.map((file, idx) => (
              <div key={idx} className="relative bg-slate-100 px-3 py-1 rounded-lg text-xs flex items-center">
                <span>{file.name}</span>
                <button type="button" onClick={() => removeNewFile(idx)} className="ml-2 text-red-500">
                  <X size={14} />
                </button>
              </div>
            ))}
          </div>
        </div>

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