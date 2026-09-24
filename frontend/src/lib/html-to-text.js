// 목록 미리보기용 — 리치텍스트로 저장된 글의 태그를 지우고 텍스트만 남긴다.
// 목록에서 보여주는 값은 항상 상세 페이지에서 다시 안전하게 렌더링되므로 여긴 그냥 정규식으로
// 충분하다(이 결과를 dangerouslySetInnerHTML 같은 곳에 다시 넣지 않는 한).
export function htmlToText(html) {
  if (!html) return "";
  return html
    .replace(/<(p|br|li|h[1-6]|blockquote)[^>]*>/gi, " ")
    .replace(/<[^>]*>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/\s+/g, " ")
    .trim();
}
