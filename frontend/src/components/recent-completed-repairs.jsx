import Link from "next/link";
import { Wrench } from "@phosphor-icons/react/dist/ssr";
import { imageSrc } from "@/lib/backend";

const categoryLabel = {
  ALL: "전체",
  ELECTRIC_LIGHT: "전기·조명",
  PLUMBING: "배관·설비",
  FURNITURE_INSTALL: "가구·설치",
  HOME_APPLIANCE: "가전제품",
  DOOR_WINDOW: "문·창문",
  LIVING_ETC: "생활·기타",
};

export default function RecentCompletedRepairs({ posts }) {
  if (!posts?.length) {
    return null;
  }

  return (
    <section className="reference-card recent-completed-section">
      <div className="reference-card-heading">
        <h2>최근 해결된 수리</h2>
      </div>

      <div className="recent-completed-grid">
        {posts.map((post) => (
          <Link
            key={post.id}
            href={`/posts/${post.id}`}
            className="recent-completed-card"
          >
            {post.thumbnailUrl ? (
              <img
                src={imageSrc(post.thumbnailUrl)}
                alt={post.title}
              />
            ) : (
              <div className="recent-completed-placeholder">
                <Wrench size={32} weight="duotone" />
              </div>
            )}

            <div>
              <span className="completed-badge">
                수리 완료
              </span>

              <h3>{post.title}</h3>

              <p>
                {categoryLabel[post.category] ?? post.category}
              </p>

              <span>
                {post.regionName}
              </span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
