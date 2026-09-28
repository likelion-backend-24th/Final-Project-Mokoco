import Link from "next/link";
import {
  Desktop,
  DeviceMobile,
  Lightbulb,
  Drop,
  Armchair,
  DotsThree,
} from "@phosphor-icons/react/dist/ssr";

const categories = [
  {
    label: "컴퓨터/IT",
    description: "PC, 노트북, 모니터 등",
    value: "COMPUTER",
    icon: Desktop,
    className: "category-blue",
  },
  {
    label: "가전",
    description: "세탁기, 냉장고, TV 등",
    value: "APPLIANCE",
    icon: DeviceMobile,
    className: "category-pink",
  },
  {
    label: "전기/조명",
    description: "전등, 콘센트, 스위치 등",
    value: "ELECTRIC",
    icon: Lightbulb,
    className: "category-yellow",
  },
  {
    label: "배관",
    description: "누수, 수도, 배수구 등",
    value: "PLUMBING",
    icon: Drop,
    className: "category-green",
  },
  {
    label: "가구",
    description: "가구 조립, 수리, 설치 등",
    value: "FURNITURE",
    icon: Armchair,
    className: "category-purple",
  },
  {
    label: "기타",
    description: "그 외 다양한 수리",
    value: "ETC",
    icon: DotsThree,
    className: "category-gray",
  },
];

export default function RepairCategorySection({ regionScope }) {
  return (
    <section className="reference-card repair-category-section">
      <div className="reference-card-heading">
        <h2>어떤 수리가 필요하세요?</h2>

        <Link
          href={`/posts?regionScope=${regionScope ?? "ALL"}`}
          className="category-all-link"
        >
          전체 카테고리 보기
        </Link>
      </div>

      <div className="repair-category-grid">
        {categories.map(
          ({
            label,
            description,
            value,
            icon: Icon,
            className,
          }) => (
            <Link
              key={value}
              href={`/posts?category=${value}&regionScope=${regionScope ?? "ALL"}`}
              className={`repair-category-item ${className}`}
            >
              <div className="repair-category-icon">
                <Icon size={34} weight="duotone" />
              </div>

              <strong>{label}</strong>
              <span>{description}</span>
            </Link>
          )
        )}
      </div>
    </section>
  );
}
