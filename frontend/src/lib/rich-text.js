function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function plainTextToHtml(value = "") {
  if (!value) return "<p></p>";
  return value.split(/\r?\n/).map((line) => `<p>${line ? escapeHtml(line) : "<br>"}</p>`).join("");
}