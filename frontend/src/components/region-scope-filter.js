import Link from "next/link";
import { MapPin } from "@phosphor-icons/react/dist/ssr";
import { REGION_SCOPES, regionListHref } from "@/lib/region-scope";

export default function RegionScopeFilter({ regionScope = "SIDO", regionFilter, category = "ALL", pathname = "/posts" }) {
  return (
    <nav aria-label="지역 조회 범위" className="region-scope-nav">
      <div className="region-scope-heading">
        <MapPin size={18} weight="duotone" />
        <span>지역 필터</span>
        <span className="region-scope-hint">활동 지역 기준으로 범위를 선택하세요</span>
      </div>
      <div className="region-scope-options">
        {REGION_SCOPES.map(({ value, label, field }) => (
          <Link
            key={value}
            href={regionListHref({ pathname, category, regionScope: value })}
            aria-current={regionScope === value ? "page" : undefined}
            className={`region-scope-chip ${regionScope === value ? "region-scope-chip-active" : ""}`}
          >
            <span className="region-scope-label">{label}{value === "SIDO" ? " · 기본" : ""}</span>
            <span className="region-scope-value">{regionFilter?.[field] ? `${regionFilter[field]}${value === "DONG" ? "" : " 전체"}` : `${label} 범위`}</span>
          </Link>
        ))}
      </div>
    </nav>
  );
}
