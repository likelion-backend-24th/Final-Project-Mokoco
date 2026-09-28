import Link from "next/link";
import {
  Armchair,
  Door,
  DotsThree,
  Drop,
  Lightbulb,
  Television,
} from "@phosphor-icons/react/dist/ssr";

const categories = [
  {
    label: "전기·조명",
    description: "전등, 콘센트, 스위치 등",
    value: "ELECTRIC_LIGHT",
    icon: Lightbulb,
    className: "category-yellow",
  },
  {
    label: "배관·설비",
    description: "누수, 수도, 배수구 등",
    value: "PLUMBING",
    icon: Drop,
    className: "category-green",
  },
  {
    label: "가구·설치",
    description: "가구 조립, 설치, 수리 등",
    value: "FURNITURE_INSTALL",
    icon: Armchair,
    className: "category-purple",
  },
  {
    label: "가전제품",
    description: "세탁기, 냉장고, TV 등",
    value: "HOME_APPLIANCE",
    icon: Television,
    className: "category-pink",
  },
  {
    label: "문·창문",
    description: "문, 도어락, 창문 등",
    value: "DOOR_WINDOW",
    icon: Door,
    className: "category-blue",
  },
  {
    label: "생활·기타",
    description: "그 외 다양한 생활 수리",
    value: "LIVING_ETC",
    icon: DotsThree,
    className: "category-gray",
  },
];

export default function RepairCategorySection({ regionScope }) {
  const scope = regionScope ?? "ALL";

  return (
    <section className="reference-card repair-category-section">
      <div className="reference-card-heading">
        <h2>어떤 수리가 필요하세요?</h2>

        <Link
          href={`/posts?category=ALL&regionScope=${scope}`}
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
              href={`/posts?category=${value}&regionScope=${scope}`}
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
