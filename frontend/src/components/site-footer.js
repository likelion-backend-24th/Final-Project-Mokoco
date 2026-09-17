"use client";

const POLICY_LINKS = [
  { label: "이용약관", path: "/policy/terms.html" },
  { label: "개인정보처리방침", path: "/policy/privacy.html" },
  { label: "운영정책", path: "/policy/operation.html" },
  { label: "위치기반서비스 이용약관", path: "/policy/location.html" },
  { label: "이용자보호 비전과 계획", path: "/policy/user-protection.html" },
  { label: "청소년보호정책", path: "/policy/youth.html" },
];

// 정책 페이지는 user-service가 정적 리소스로 서빙하고 Caddy가 /policy/* 를 그쪽으로 직결한다
// (infra/Caddyfile 참고) — 브라우저 기준 같은 오리진 상대경로라 backendUrl() 없이 그대로 링크한다.
export default function SiteFooter() {
  return (
    <footer className="site-footer-policy">
      <div className="hosting-info">
        <strong>호스팅 사업자</strong> Amazon Web Service(AWS)
      </div>
      <nav className="policy-links">
        {POLICY_LINKS.map((item) => (
          <a
            key={item.path}
            href={item.path}
            target="_blank"
            rel="noopener noreferrer"
          >
            {item.label}
          </a>
        ))}
      </nav>

      <style jsx>{`
        .site-footer-policy {
          max-width: 1080px;
          margin: 60px auto 0;
          padding: 24px 32px 48px;
          color: #6b7280;
          font-size: 14px;
          line-height: 1.7;
          border-top: 1px solid #e5e7eb;
        }
        .hosting-info {
          margin-bottom: 12px;
        }
        .hosting-info strong {
          color: #1f2937;
          font-weight: 700;
          margin-right: 4px;
        }
        .policy-links {
          display: flex;
          flex-wrap: wrap;
          gap: 4px 20px;
        }
        .policy-links a {
          color: #6b7280;
          text-decoration: none;
          font-size: 14px;
        }
        .policy-links a:hover {
          color: #1f2937;
          text-decoration: underline;
        }
      `}</style>
    </footer>
  );
}
