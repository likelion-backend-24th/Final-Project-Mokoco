"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ImageSquare, X } from "@phosphor-icons/react";

const MAX_IMAGES = 5;

export default function PostImageManager({ postId, images, isMine }) {
  const router = useRouter();
  const [uploading, setUploading] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [message, setMessage] = useState("");

  if (!isMine && images.length === 0) return null;

  async function handleAddImages(event) {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    if (files.length === 0) return;

    setMessage("");
    setUploading(true);
    try {
      const formData = new FormData();
      files.forEach((file) => formData.append("images", file));

      const response = await fetch(`/api/posts/${postId}/images`, {
        method: "POST",
        credentials: "include",
        body: formData,
      });
      const payload = await response.json();

      if (!response.ok) {
        setMessage(payload.message ?? "사진을 추가하지 못했습니다.");
        return;
      }
      router.refresh();
    } catch {
      setMessage("서버와 통신할 수 없습니다.");
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(imageId) {
    setMessage("");
    setDeletingId(imageId);
    try {
      const response = await fetch(`/api/posts/${postId}/images/${imageId}`, {
        method: "DELETE",
        credentials: "include",
      });
      const payload = await response.json();

      if (!response.ok) {
        setMessage(payload.message ?? "사진을 삭제하지 못했습니다.");
        return;
      }
      router.refresh();
    } catch {
      setMessage("서버와 통신할 수 없습니다.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="post-image-manager">
      {images.length > 0 && (
        <div className="post-image-gallery">
          {images.map((image) => (
            <div key={image.id} className="post-image-gallery-item">
              <img src={image.imageUrl} alt="" />
              {isMine && (
                <button type="button" onClick={() => handleDelete(image.id)} disabled={deletingId === image.id} aria-label="사진 삭제">
                  {deletingId === image.id ? "..." : <X size={14} weight="bold" />}
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      {isMine && images.length < MAX_IMAGES && (
        <label className="repair-image-upload">
          <ImageSquare size={20} weight="bold" />
          {uploading ? "업로드 중..." : "사진 추가"}
          <input type="file" accept="image/*" multiple onChange={handleAddImages} disabled={uploading} hidden />
        </label>
      )}

      {message && <p className="form-message form-message-error">{message}</p>}
    </div>
  );
}
