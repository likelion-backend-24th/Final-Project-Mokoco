# Mokoco Frontend

Mokoco의 Next.js 프론트엔드 프로젝트입니다.

- Next.js 16 App Router
- React 19
- JavaScript
- Tailwind CSS 4
- ESLint

## 실행 방법

의존성을 설치하고 개발 서버를 실행합니다.

```bash
npm install
npm run dev
```

[http://localhost:3000](http://localhost:3000)에서 확인할 수 있습니다.

## 환경변수

`.env.example`을 복사해 `.env.local`을 생성합니다.

```powershell
Copy-Item .env.example .env.local
```

기본 API Gateway 주소는 `http://localhost:8000`입니다. 브라우저는 Next.js Route Handler를 통해 같은 출처로 요청하고, Route Handler가 Gateway로 전달합니다.

로그인 후 저장된 지역이 없으면 위치 권한 안내가 표시됩니다. 승인된 GPS 좌표는 `PATCH /api/users/me/region`으로 전달되며, 백엔드는 VWorld 역지오코딩 결과로 사용자의 행정구역 코드를 갱신합니다.

## 명령어

```bash
npm run dev
npm run lint
npm run build
npm run start
```
