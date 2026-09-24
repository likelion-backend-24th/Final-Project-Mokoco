// 리치텍스트 에디터가 도입되기 전 순수 텍스트로 저장된 글을 수정할 때, 원래 줄바꿈을
// 유지한 채로 에디터에 불러오기 위한 변환. 빈 줄(\n\n)은 문단 구분, 단일 줄바꿈은 <br>로 바꾼다.
// 원문에 <, & 같은 문자가 있어도 마크업으로 오인되지 않도록 먼저 escape한다.
function escapeHtml(text) {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

export function plainTextToHtml(text) {
  if (!text) return "";
  return text
    .split(/\n{2,}/)
    .map((paragraph) => `<p>${escapeHtml(paragraph).split("\n").join("<br>")}</p>`)
    .join("");
}
