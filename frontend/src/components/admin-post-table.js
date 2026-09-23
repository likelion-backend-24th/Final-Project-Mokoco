"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { CaretLeft, CaretRight, MagnifyingGlass, Trash } from "@phosphor-icons/react";

const STATUS_OPTIONS = [
  { value: "", label: "전체" },
  { value: "WAITING", label: "도움 기다리는 중" },
  { value: "MATCHED", label: "이웃과 연결됨" },
  { value: "COMPLETED", label: "거래 완료" },
];
const STATUS_LABEL = { WAITING: "도움 기다리는 중", MATCHED: "이웃과 연결됨", COMPLETED: "거래 완료" };
const STATUS_STYLE = {
  WAITING: "bg-blue-100 text-blue-700",
  MATCHED: "bg-amber-100 text-amber-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
};
const CATEGORY_LABEL = {
  ELECTRIC_LIGHT: "전기·조명",
  PLUMBING: "배관·설비",
  FURNITURE_INSTALL: "가구·설치",
  HOME_APPLIANCE: "가전제품",
  DOOR_WINDOW: "문·창문",
  LIVING_ETC: "생활·기타",
};

export default function AdminPostTable({ initialPage }) {
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [pageData, setPageData] = useState(initialPage);
  const [page, setPage] = useState(initialPage?.number ?? 0);
  const [loading, setLoading] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const isFirstRun = useRef(true);

  // 입력마다 바로 요청하지 않고 타이핑이 멈춘 뒤 300ms 후에 검색어를 반영한다.
  useEffect(() => {
    const timer = setTimeout(() => {
      setKeyword(keywordInput);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [keywordInput]);

  useEffect(() => {
    if (isFirstRun.current) {
      isFirstRun.current = false;
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError("");
    const query = new URLSearchParams({ page: String(page), size: "20" });
    if (status) query.set("status", status);
    if (keyword) query.set("keyword", keyword);

    fetch(`/api/admin/posts?${query.toString()}`, { signal: controller.signal, cache: "no-store" })
      .then(async (res) => {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error ?? "글 목록을 불러오지 못했습니다.");
        setPageData(json);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message ?? "서버에 연결할 수 없습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [status, keyword, page]);

  function changeStatus(value) {
    setStatus(value);
    setPage(0);
  }

  function goToPage(next) {
    setPage(next);
  }

  async function handleDelete(post) {
    if (!window.confirm(`"${post.title}" 글을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`)) return;
    setDeletingId(post.id);
    setError("");
    setNotice("");
    try {
      const response = await fetch(`/api/admin/posts/${post.id}`, { method: "DELETE" });
      if (!response.ok && response.status !== 204) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error || "삭제에 실패했습니다.");
      }
      setPageData((current) => ({
        ...current,
        content: current.content.filter((item) => item.id !== post.id),
        totalElements: Math.max(0, current.totalElements - 1),
      }));
      setNotice("글을 삭제했어요.");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setDeletingId(null);
    }
  }

  const posts = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 1;

  return (
    <div className="dashboard-card">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="relative">
          <MagnifyingGlass size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={keywordInput}
            onChange={(event) => setKeywordInput(event.target.value)}
            placeholder="제목으로 검색"
            className="rounded-lg border border-slate-200 py-2 pl-9 pr-3 text-sm outline-none focus:border-blue-400"
          />
        </div>
        <select
          value={status}
          onChange={(event) => changeStatus(event.target.value)}
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-600 outline-none focus:border-blue-400"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
        <span className="ml-auto text-xs text-slate-400">
          {pageData ? `총 ${pageData.totalElements.toLocaleString("ko-KR")}건` : ""}
        </span>
      </div>

      {notice && <p role="status" className="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{notice}</p>}
      {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>}

      {posts.length === 0 ? (
        <div className="py-10 text-center text-sm text-slate-400">
          {loading ? "불러오는 중..." : "해당하는 글이 없어요."}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full table-fixed text-left text-sm">
            <colgroup>
              <col className="w-[28%]" />
              <col className="w-[16%]" />
              <col className="w-[12%]" />
              <col className="w-[13%]" />
              <col className="w-[9%]" />
              <col className="w-[13%]" />
              <col className="w-[9%]" />
            </colgroup>
            <thead>
              <tr className="border-b border-slate-200 text-xs text-slate-400">
                <th className="py-2 pr-3 font-semibold">글</th>
                <th className="py-2 pr-3 font-semibold">작성자</th>
                <th className="py-2 pr-3 font-semibold">카테고리</th>
                <th className="py-2 pr-3 font-semibold">상태</th>
                <th className="py-2 pr-3 font-semibold">공개</th>
                <th className="py-2 pr-3 font-semibold">작성일</th>
                <th className="py-2 pr-3 font-semibold"></th>
              </tr>
            </thead>
            <tbody>
              {posts.map((post) => (
                <tr key={post.id} className="border-b border-slate-100 last:border-0">
                  <td className="py-2.5 pr-3">
                    <Link
                      href={`/posts/${post.id}`}
                      title={post.title}
                      className="block truncate font-semibold text-blue-600 hover:underline"
                    >
                      {post.title}
                    </Link>
                  </td>
                  <td className="truncate py-2.5 pr-3 text-slate-500" title={post.authorNickname ?? post.authorEmail ?? undefined}>
                    {post.authorNickname ?? post.authorEmail ?? "—"}
                  </td>
                  <td className="truncate py-2.5 pr-3 text-slate-500">{CATEGORY_LABEL[post.category] ?? post.category}</td>
                  <td className="whitespace-nowrap py-2.5 pr-3">
                    <span className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-bold ${STATUS_STYLE[post.status] ?? "bg-slate-100 text-slate-500"}`}>
                      {STATUS_LABEL[post.status] ?? post.status}
                    </span>
                  </td>
                  <td className="py-2.5 pr-3 text-slate-500">{post.publiclyVisible ? "공개" : "비공개"}</td>
                  <td className="py-2.5 pr-3 text-xs text-slate-400">
                    {post.createdAt ? new Date(post.createdAt).toLocaleDateString("ko-KR") : "—"}
                  </td>
                  <td className="py-2.5 pr-3">
                    <button
                      type="button"
                      onClick={() => handleDelete(post)}
                      disabled={deletingId !== null}
                      aria-label="글 삭제"
                      className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-2 py-1 text-xs font-semibold text-red-600 hover:bg-red-50 disabled:opacity-40"
                    >
                      <Trash size={13} weight="bold" />
                      {deletingId === post.id ? "삭제 중..." : "삭제"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-3">
          <button
            type="button"
            onClick={() => goToPage(Math.max(page - 1, 0))}
            disabled={page === 0 || loading}
            className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50 disabled:opacity-40"
          >
            <CaretLeft size={14} weight="bold" />
            이전
          </button>
          <span className="text-xs text-slate-400">{page + 1} / {totalPages}</span>
          <button
            type="button"
            onClick={() => goToPage(Math.min(page + 1, totalPages - 1))}
            disabled={page >= totalPages - 1 || loading}
            className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-50 disabled:opacity-40"
          >
            다음
            <CaretRight size={14} weight="bold" />
          </button>
        </div>
      )}
    </div>
  );
}
