export function formatRelativeDate(value) {
  if (!value) return "시간 정보 없음";

  let date;
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0, s = 0] = value;
    date = new Date(y, m - 1, d, h, min, s);
  } else {
    date = new Date(value);
  }

  if (Number.isNaN(date.getTime())) return "시간 정보 없음";

  const minutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  return hours < 24 ? `${hours}시간 전` : `${Math.floor(hours / 24)}일 전`;
}
