import "./globals.css";
import "leaflet/dist/leaflet.css";

export const metadata = {
  title: "Mokoco",
  description: "가까운 이웃과 함께하는 지역 커뮤니티",
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
