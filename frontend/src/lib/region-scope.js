// ALL = 필터 미적용(기본값, 전체 글 노출). 직접 눌러서 골랐을 때만 시도/시군구/읍면동으로 좁혀진다.
export const REGION_SCOPES = [
  { value: "ALL", label: "전체", field: null },
  { value: "SIDO", label: "시·도", field: "sido" },
  { value: "SIGUNGU", label: "시·군·구", field: "sigungu" },
  { value: "DONG", label: "읍·면·동", field: "dong" },
];

export function normalizeRegionScope(value) {
  return REGION_SCOPES.some((scope) => scope.value === value) ? value : "ALL";
}

export function regionListHref({ pathname = "/posts", category = "ALL", regionScope = "ALL", page = 0 } = {}) {
  const query = new URLSearchParams({ regionScope: normalizeRegionScope(regionScope) });
  if (category !== "ALL") query.set("category", category);
  if (page > 0) query.set("page", String(page));
  return `${pathname}?${query}${pathname === "/" ? "#posts" : ""}`;
}
