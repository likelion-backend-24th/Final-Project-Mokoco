// 기본값은 내 시·군·구 — 서울 사는 사람이 제주도 글을 기본으로 볼 필요는 없다.
// "전체"는 사용자가 직접 눌러서 고를 때만 적용된다.
export const REGION_SCOPES = [
  { value: "ALL", label: "전체", field: null },
  { value: "SIDO", label: "시·도", field: "sido" },
  { value: "SIGUNGU", label: "시·군·구", field: "sigungu" },
  { value: "DONG", label: "읍·면·동", field: "dong" },
];

export function normalizeRegionScope(value) {
  return REGION_SCOPES.some((scope) => scope.value === value) ? value : "SIGUNGU";
}

export function regionListHref({ pathname = "/posts", category = "ALL", regionScope = "ALL", page = 0 } = {}) {
  const query = new URLSearchParams({ regionScope: normalizeRegionScope(regionScope) });
  if (category !== "ALL") query.set("category", category);
  if (page > 0) query.set("page", String(page));
  return `${pathname}?${query}${pathname === "/" ? "#posts" : ""}`;
}
