import Link from "next/link";
import { MapPin } from "@phosphor-icons/react/dist/ssr";
import { REGION_SCOPES, regionListHref } from "@/lib/region-scope";

export default function RegionScopeFilter({ regionScope = "SIDO", regionFilter, category = "ALL", pathname = "/posts" }) {
  return (
    <nav aria-label="지역 조회 범위" className="mb-5 rounded-2xl border border-slate-200 bg-white p-4">
      <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
        <MapPin size={18} weight="duotone" />
        <span>지역 필터</span>
        <span className="font-normal text-slate-500">활동 지역 기준으로 범위를 선택하세요</span>
      </div>
      <div className="flex flex-wrap gap-2">
        {REGION_SCOPES.map(({ value, label, field }) => (
          <Link
            key={value}
            href={regionListHref({ pathname, category, regionScope: value })}
            aria-current={regionScope === value ? "page" : undefined}
            className={`category-filter inline-flex min-w-[112px] flex-col items-start gap-1 px-4 py-3 ${regionScope === value ? "category-filter-active" : ""}`}
          >
            <span className="text-xs">{label}{value === "SIDO" ? " · 기본" : ""}</span>
            <span>{regionFilter?.[field] ? `${regionFilter[field]}${value === "DONG" ? "" : " 전체"}` : `${label} 범위`}</span>
          </Link>
        ))}
      </div>
    </nav>
  );
}
