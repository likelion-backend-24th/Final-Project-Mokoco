export const REGION_SCOPES = [
  { value: "SIDO", label: "시·도", field: "sido" },
  { value: "SIGUNGU", label: "시·군·구", field: "sigungu" },
  { value: "DONG", label: "읍·면·동", field: "dong" },
];

export function normalizeRegionScope(value) {
  return REGION_SCOPES.some((scope) => scope.value === value) ? value : "SIDO";
}

export function regionListHref({ pathname = "/posts", category = "ALL", regionScope = "SIDO", page = 0 } = {}) {
  const query = new URLSearchParams({ regionScope: normalizeRegionScope(regionScope) });
  if (category !== "ALL") query.set("category", category);
  if (page > 0) query.set("page", String(page));
  return `${pathname}?${query}${pathname === "/" ? "#posts" : ""}`;
}
