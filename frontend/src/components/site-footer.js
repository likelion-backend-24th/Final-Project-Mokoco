const POLICY_LINKS = [
  { label: "이용약관", path: "/policy/terms.html" },
  { label: "개인정보처리방침", path: "/policy/privacy.html" },
  { label: "운영정책", path: "/policy/operation.html" },
  { label: "위치기반서비스 이용약관", path: "/policy/location.html" },
  { label: "이용자보호 비전과 계획", path: "/policy/user-protection.html" },
  { label: "청소년보호정책", path: "/policy/youth.html" },
];

// 정책 페이지는 user-service가 정적 리소스로 서빙하고 Caddy가 /policy/* 를 그쪽으로 직결한다
// (infra/Caddyfile 참고) — 브라우저 기준 같은 오리진 상대경로라 그대로 링크한다.
export default function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="page-shell footer-inner">
        <div>
          <strong>동네수리</strong>
          <p>© 2026 동네수리. All rights reserved.</p>
          <p>호스팅 사업자: Amazon Web Service(AWS)</p>
        </div>
        <div className="footer-links">
          {POLICY_LINKS.map((item) => (
            <a key={item.label} href={item.path}>
              {item.label}
            </a>
          ))}
        </div>
      </div>
    </footer>
  );
}
