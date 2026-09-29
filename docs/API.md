# API·통합 계약

### 공통 규약

- 외부(브라우저) 호출 Prefix: `/api/*` 가 기본이지만, Next.js가 SSR에서 게이트웨이로 직접 호출하는 일부 경로(`/posts/**`, `/fix-deals/**`, `/notifications/**`, `/reviews/**`, `/profile/**`, `/resumes/**`, `/payments/**`)는 접두어 없이 그대로 라우팅됩니다. 내부 서비스 간 호출: `/internal/*` 또는 `/api/internal/*` (외부 미공개)
- **인증은 `Authorization: Bearer {accessToken}` 하나로 통일되어 있습니다.** `X-User-Email` 헤더는 완전히 폐기됐습니다 — post-service·payment-service·chat-service 공통으로 `TokenAuthenticationFilter`가 Bearer 토큰을 user-service의 `verify-token`으로 검증해 `LoginUser(id, email)`를 심고, 코드 주석에도 "더 이상 X-User-Email을 신뢰하지 않는다"고 명시돼 있습니다. (예전 문서에 남아있던 "레거시 X-User-Email / 신규 Bearer 혼재" 설명은 더 이상 사실이 아닙니다 — `git grep X-User-Email`으로 전체 backend를 검색하면 payment-service `SecurityConfig`의 주석 한 줄에서만 나오고, 그마저도 "더 이상 안 믿는다"는 내용입니다.)
- 실패 응답은 대부분 `{"error": "메시지"}` 형태(도메인 예외). 인증 필터 자체가 막는 경우(토큰 없음/무효)는 `{"code": "...", "message": "...", "requestId": "..."}` 형태로 조금 다릅니다.
- 인증 없음/무효 401, 권한 부족 403, 리소스 없음 404, 상태 충돌 409
- 브라우저는 직접 백엔드를 호출하지 않고 대부분 Next.js BFF(`/api/*` route handler)를 거쳐 쿠키의 토큰을 헤더로 주입받습니다. 일부(멀티파트 업로드, 결제 웹훅, WebSocket)는 Caddy가 게이트웨이를 건너뛰고 해당 서비스로 직결합니다.

---

# Mokoco API 명세 (release 브랜치 기준)

## 1. 인증 — user-service

- `POST /api/auth/signup` — 회원가입

    Req: `email, password, name, nickname, regionCode(선택)`

    - 비밀번호: 8자 이상, 영문·숫자 모두 포함 (정규식 검증)
    - 성공 시 생성된 `userId` 반환, `200 OK`
    - 이메일/닉네임 중복 시 오류
- `POST /api/auth/signin` — 로그인

    Req: `email, password`

    - 성공 시 Access Token / Refresh Token 반환
    - JWT Subject는 `email`이 아니라 `userId`
- `POST /api/auth/reissue` — Access Token 재발급

    Req: `refreshToken`

    - Refresh Token 내부의 `userId` 기준으로 사용자 식별
- `/oauth2/authorization/{provider}` → `/login/oauth2/code/{provider}` — 소셜 로그인
    - Spring Security OAuth2 표준 플로우
    - 지원 Provider: Google, Kakao

---

## 2. 내 정보 / 활동 지역 — user-service

- `GET /api/users/me` — 내 정보 조회 (Bearer)
- `GET /api/users/me/social-accounts` — 연결된 소셜 로그인 provider 목록 (예: `["GOOGLE"]`)
- `PATCH /api/users/me` — 이름/닉네임 수정

    Req: `name, nickname`

- `PATCH /api/users/me/password` — 비밀번호 변경

    Req: `currentPassword, newPassword`

- `GET /api/users/me/region` — 내 활동 지역 조회
    - 지역 미설정 시 `404`
- `PATCH /api/users/me/region` — 활동 지역 생성/변경

---

## 3. 게시글 — post-service

> Gateway 기준 외부 경로는 `/posts/**` (SSR은 접두어 없이, 브라우저는 `/api/posts/**` → StripPrefix)
>
- `POST /posts` — 게시글 작성 (Bearer)

    `multipart/form-data`

    - part `post`: `title, content, category`
    - part `images` — 선택, 여러 장 가능
    - 가격은 게시글에 저장하지 않고 Proposal 단계에서 결정. 지역은 입력값이 아니라 작성자의 활동 지역에서 서버가 자동으로 채움.

- `GET /posts` — 게시글 목록 조회 (비로그인 허용, 로그인 시 개인화)

    Query:

    - `category` — 선택 (`PostCategory`)
    - `regionScope` — `ALL | SIDO | SIGUNGU | DONG`, 기본값 `ALL`(필터 없음 — 이전 문서에 "ALL이 없다"고 되어 있었는데 실제로는 **기본값이 ALL**입니다)
    - `page` 기본 0, `size` 기본 20

- `GET /posts/{id}` — 게시글 상세 조회 (공개)
- `PATCH /posts/{id}` — 게시글 수정 (Bearer, 작성자만) — Req: `title, content, category`
- `DELETE /posts/{id}` — 게시글 삭제 (Bearer, 작성자만)
- `PATCH /posts/{id}/visibility` — 공개 여부 변경 (Bearer) — Req: `publiclyVisible`
- `POST /posts/{id}/images` — 이미지 추가 (Bearer, `multipart/form-data`)
- `DELETE /posts/{id}/images/{imageId}` — 이미지 삭제 (Bearer)
- `GET /posts/completed/recent` — 최근 완료된 수리 목록 (공개) — Query: `size`(기본 3)

### PostStatus

```
WAITING → MATCHED → COMPLETED
```

### PostCategory

```
ALL
ELECTRIC_LIGHT
PLUMBING
FURNITURE_INSTALL
HOME_APPLIANCE
DOOR_WINDOW
LIVING_ETC
```

---

## 4. 제안 — post-service

- `POST /posts/{postId}/proposals` — 제안 등록 (Bearer)

    Req: `estimatedPrice, content, attachResume(선택)`

- `GET /posts/{postId}/proposals` — 제안 목록 조회 (공개 — 인증 불필요)
- `PATCH /posts/{postId}/proposals/{proposalId}/adopt` — 제안 채택 (Bearer, 게시글 작성자만)
    - `PostStatus → MATCHED`, `FixDeal` 생성
    - 중복/동시 채택은 비관적 락 + DB Unique 제약으로 제어
- `PATCH /posts/{postId}/proposals/{proposalId}/cancel` — 채택 취소 (Bearer) — 결제 전(MATCHED)에만 가능
- `DELETE /posts/{postId}/proposals/{proposalId}` — 제안 삭제 (Bearer, 제안 작성자 본인만) — 채택됨/채팅방 존재 시 409

> 제안 수정 API는 없음. 본인 게시글에 본인이 제안하는 것도 서버가 막지 않음(문서상 규칙과 실제 구현이 다를 수 있는 지점).
>

---

## 5. 거래 — FixDeal / post-service

> **FixDealController는 읽기 전용입니다.** 예전에 있던 개별 상태전이 엔드포인트(`/fix-deals/{id}/product-sent`, `/repairing`, `/repair-done`, `/complete`)는 전부 삭제됐고, 상태 전이는 전부 아래 "6. 계약서"의 통합 액션(`ContractService.advance()`) 하나로만 이뤄집니다.
>

- `GET /fix-deals/{fixDealId}` — 거래 상세 조회 (Bearer, 참여자만)
- `GET /posts/{postId}/fix-deal` — 게시글 기준 FixDeal 상태 조회 (공개)

### FixDealStatus

```
MATCHED → REPAIRING → REPAIR_DONE → COMPLETED
CANCELED
```

`PRODUCT_SENT`는 결제를 미리 받는 흐름 도입 이전의 레거시 상태로, Enum에는 남아 있고 `finish` 액션의 출발 상태로도 계속 허용되지만 새 거래에서는 발생하지 않습니다.

---

## 6. 계약서 — post-service

계약은 ChatRoom ID 기준으로 관리하고, 계약 데이터 자체는 post-service가 소유합니다.

### 계약 조회

- `GET /api/chat-rooms/{roomId}/contract` (Bearer) — 현재 계약 버전, 서명 상태, 거래/결제 개요 반환

### 계약 초안 작성

- `POST /api/chat-rooms/{roomId}/contract` (Bearer)

    Req: `baseId`(최초면 null, 수정이면 기준 버전 id), `terms`(`ContractTerms`)

    - `ContractTerms.totalAmount`는 **정수 원 단위만 허용**(소수점 불가 — `@Digits(fraction=0)`)

### 계약 상태 처리 (통합 액션)

- `POST /api/chat-rooms/{roomId}/contract/{action}` (Bearer)

    허용 Action: `request | sign | start | finish | accept`

    Req: `versionId, documentHash, signerName, consent`

    - `request` — 서명 요청
    - `sign` — 전자 서명
    - `start` — 작업 시작 (**수리자**가 호출, `MATCHED`에서만, 의뢰인 결제 완료 필요 — 안 되면 409)
    - `finish` — 완료 신청 (**수리자**가 호출, `REPAIRING`/`PRODUCT_SENT`에서만 → `REPAIR_DONE`)
    - `accept` — 완료 확정 (**요청자(의뢰인)**가 호출, `REPAIR_DONE`에서만 → `COMPLETED`, 게시글도 완료 처리, payment-service에 정산 확정(`settle`) 내부 호출까지 트랜잭션 안에서 수행)

---

## 7. 채팅방 생성/연결 — post-service orchestration

ChatRoom Entity/ChatMessage는 chat-service 소유지만, Proposal/FixDeal 컨텍스트를 아는 post-service가 생성에 필요한 정보를 구성해 chat-service를 호출합니다.

- `POST /api/chat-rooms/proposals/{proposalId}` (Bearer) — 제안 기준 채팅방 생성/기존 방 반환
- `GET /api/chat-rooms/proposals/{proposalId}` (Bearer) — 제안 기준 채팅방 조회
- `POST /api/chat-rooms/fix-deals/{fixDealId}` (Bearer) — FixDeal 기준 채팅방 생성/연결
- `GET /api/chat-rooms/fix-deals/{fixDealId}` (Bearer) — FixDeal 기준 채팅방 조회
- `GET /api/chat-rooms/{roomId}/detail` (Bearer) — ChatRoom + 거래 정보 조합 반환

---

## 8. 채팅 / 메시지 / 첨부파일 — chat-service

- `GET /api/chat-rooms` (Bearer) — 내 채팅방 목록. Query: `page`(기본 0), `size`(기본 5)
- `GET /api/chat-rooms/{roomId}/counterpart` (Bearer) — 상대방 `id`, `nickname`
- `GET /api/chat-rooms/session` (Bearer) — 현재 Access Token 기준 사용자 검증 정보
- `GET /api/chat-rooms/{roomId}/messages` (Bearer) — Query: `before`(선택, 메시지 ID 커서)
- `DELETE /api/chat-rooms/{roomId}/messages/{messageId}` (Bearer) — 본인 메시지만, soft delete
- `POST /api/chat-rooms/{roomId}/attachments` (Bearer, `multipart/form-data`) — Req: `file`
- `GET /api/chat-rooms/{roomId}/attachments/{messageId}` (Bearer) — 다운로드

### WebSocket

```
Endpoint:   /ws/chat
Send:       /app/chat/{roomId}
Subscribe:  /topic/chat/{roomId}
개인 알림 구독:  /user/queue/notifications
```

- CONNECT 시 `Authorization: Bearer` 필수, SEND/SUBSCRIBE마다 토큰 재검증
- 메시지 송신 시 사용자 식별은 WebSocket Principal의 `userId`

### MessageType

```
TEXT, IMAGE, VIDEO, SYSTEM
```

---

## 9. 결제 — payment-service

> Gateway 외부 경로는 `/payments/**` (접두어 없이 그대로)
>

결제는 `PaymentOrder → PortOne → Payment` 구조입니다.

- `POST /payments/prepare` (Bearer) — 결제 주문 준비

    Req: `{"postId": 1}`

    서버가 post-service에서 결제 Context를 조회해 `postId, fixDealId, payerId, payeeId, baseAmount, totalAmount`를 확정. `paymentId`는 서버가 생성.

    반환: `paymentId, postId, baseAmount, totalAmount, payeeEmail`

- `POST /payments` (Bearer) — 결제 확정 (의뢰인)

    `prepare`로 만든 주문을 `paymentId`로 찾아 PortOne 실제 결제와 대조. 인증된 `payerEmail`은 클라이언트 입력이 아니라 서버의 `LoginUser`에서 가져옴.

- `GET /payments/mine` (Bearer) — **수리자 본인이 받은/받을 정산 내역** (실제로 존재합니다 — 이전 문서엔 없다고 되어 있었는데 오류)

    Query: `page`(기본 0), `size`(기본 10)

- `GET /payments/{paymentId}` (Bearer) — 결제 단건 조회 (결제자 또는 수리자 본인만)
- `GET /payments/post/{postId}` (Bearer) — 게시글 기준 결제 조회 (계약서 화면에서 결제 상태 확인용)
- `POST /payments/webhook` — PortOne Webhook (Caddy가 게이트웨이 없이 직결)

    필수 Header: `webhook-id, webhook-timestamp, webhook-signature`. 서명 검증 실패 시 401.

### PaymentStatus

```
COMPLETED, FAILED, CANCELLED
```

### Payment 엔티티 주요 필드

```
payerEmail, payeeEmail, amount, feeAmount, netAmount, status,
createdAt, paidAt, settledAt (정산 확정 시각, nullable — 이전 문서엔 이 필드가 없다고 되어 있었는데 실제로 있음)
```

> `/api/payments/post/{postId}/settle`은 **공개 API로는 없습니다.** 대신 `/internal/payments/post/{postId}/settle`로 내부 전용 존재하며, 계약 `accept` 처리 중 post-service가 트랜잭션 안에서 호출합니다.
>

---

## 10. 후기 — post-service

> Gateway 외부 경로 `/reviews/**`
>
- `POST /reviews` (Bearer, `multipart/form-data`)

    part `review`: `postId, rating, content` / part `images`(선택)

- `GET /reviews` (공개) — Query: `revieweeEmail`(이전 문서엔 `revieweeId`로 바뀌었다고 되어 있었는데, 실제로는 여전히 **`revieweeEmail`**), `page`, `size`
- `GET /reviews/{reviewId}` — 후기 단건 조회 (공개)
- `GET /reviews/exists?postId={postId}` — 해당 Post 후기 존재 여부 (공개)
- `GET /reviews/by-post?postId={postId}` — 게시글 기준 후기 조회 (공개, 실제로 존재합니다 — 이전 문서엔 없다고 되어 있었는데 오류)

후기 수정·삭제 API는 없음(작성 후 불변). 작성 기한은 거래 완료 후 3일(`ReviewService.REVIEW_DEADLINE_DAYS`).

---

## 11. 이력서 — post-service

> Gateway 외부 경로 `/resumes/**`
>
- `POST /resumes` (Bearer) — 내 이력서 작성
- `PATCH /resumes` (Bearer) — 내 이력서 수정
- `DELETE /resumes` (Bearer) — 내 이력서 삭제

    Req: `headline, introduction, skills[], careers[]`

- `GET /resumes/me` (Bearer) — 내 이력서 조회
- `GET /resumes/{email}` (공개) — 특정 사용자 이력서 조회

> 이전 문서엔 `userId` 기반(`/resumes/{userId}`)으로 바뀌었다고 되어 있었는데, 실제로는 여전히 **이메일 기반**(`/resumes/{email}`)입니다.
>

---

## 12. 프로필 / 거래 내역 — post-service

> Gateway 외부 경로 `/profile/**`
>
- `GET /profile/transactions` (Bearer) — 내 거래 내역. Query: `role=requester|repairer`, `page`, `size`
- `GET /profile/reviews` (Bearer) — 내가 작성한 후기 목록
- `GET /profile/{email}/transactions` (공개) — 특정 사용자(주로 수리자) 거래 내역. Query: `role`, `page`, `size`

> 이전 문서엔 `{userId}` 기반으로 바뀌었다고 되어 있었는데, 실제로는 여전히 **`{email}`** 입니다.
>

---

## 13. 알림 — post-service

> Gateway 외부 경로 `/notifications/**`
>
- `GET /notifications` (Bearer) — 내 알림 목록
- `GET /notifications/unread-count` (Bearer) — 읽지 않은 알림 수
- `PATCH /notifications/{id}/read` (Bearer) — 단일 알림 읽음 처리
- `PATCH /notifications/read-all` (Bearer) — 전체 읽음 처리
- `GET /notifications/settings` (Bearer) — 알림 설정 조회
- `PUT /notifications/settings` (Bearer) — 알림 설정 변경

> 이전 문서엔 사용자 식별이 `recipientEmail`이 아니라 `userId` 기준으로 바뀌었다고 되어 있었는데, 실제로는 `NotificationSetting.userEmail` / `Notification.recipientEmail` 등 여전히 **이메일 기반**입니다.
>

---

## 14. AI 작성 지원 — post-service / Gemini

- `POST /api/ai/post-draft` (Bearer, `multipart/form-data`) — 게시글 초안

    Req: `images(1~5장), title, content, category`(선택). AI 결과는 초안만 반환, 바로 저장하지 않음.

- `POST /api/ai/post-revise` (Bearer) — 게시글 초안 재작성

    Req: `draftId, content, instruction`. **최대 3회**까지 재작성 가능(`AiPostDraft.MAX_REVISIONS`). 3회 초과 시 `409 AI_REVISION_LIMIT`. 초안은 생성 후 **24시간**이 지나면 `410 AI_DRAFT_EXPIRED`.

- `POST /api/chat-rooms/{roomId}/contract/ai-draft` (Bearer) — 계약서 AI 초안

    Req: `baseId, currentTerms, instructions`. 채팅 내역은 chat-service 내부 API로 조회. 금액 등 중요 거래 데이터는 AI가 임의 결정하지 않고 서버 값(`PROPOSAL_AMOUNT`)으로 고정.

- AI 실패: 비활성화/한도초과 `503`, 응답 지연 `504`, 근거 없는 값 생성 시 `422`(계약) / `502`(`INVALID_AI_RESPONSE`)로 거절

---

## 15. 관리자 — post-service / user-service

> Gateway 외부 경로: post-service 쪽 `/api/admin/posts/**`, `/api/admin/reports/**`, `/api/admin/deals/**`, `/api/admin/overview/**`, `/api/admin/payments/**` / user-service 쪽 `/api/admin/users/**`. **이전 문서엔 관리자 API 섹션 자체가 없었습니다 — 실제로는 상당히 큰 기능입니다.**
>

### 대시보드

- `GET /api/admin/overview` (Bearer, Admin) — 전체 회원 수·오늘 가입자, 진행 중 거래 수, 신고 대기 건수

### 거래·결제 현황판

- `GET /api/admin/deals` (Bearer, Admin) — Query: `status`(선택, 기본은 진행 중만: `MATCHED/REPAIRING/REPAIR_DONE`), `page`, `size`
- `GET /api/admin/deals/summary` (Bearer, Admin) — 전체 거래 금액 / 정산된 금액 요약
- `GET /api/admin/payments` (Bearer, Admin) — 결제/정산 상세 내역. Query: `status`, `page`, `size`

### 게시글 관리

- `GET /api/admin/posts` (Bearer, Admin) — 비공개 글 포함 전체 목록. Query: `keyword`, `status`, `page`, `size`
- `DELETE /api/admin/posts/{id}` (Bearer, Admin) — 작성자 무관 강제 삭제 (진행 중인 거래가 있으면 제한)

### 신고 처리

- `GET /api/admin/reports` (Bearer, Admin)
- `PATCH /api/admin/reports/{id}/resolve` (Bearer, Admin)
- `PATCH /api/admin/reports/{id}/dismiss` (Bearer, Admin)

### 회원 관리 (user-service)

- `GET /api/admin/users` (Bearer, Admin) — 전체 회원 목록
- `PATCH /api/admin/users/{id}/role` (Bearer, Admin) — 권한 변경. Req: `role`
- `PATCH /api/admin/users/{id}/status` (Bearer, Admin) — 계정 상태 변경(정지 등). Req: `status`
- `PATCH /api/admin/users/by-email/status` (Bearer, Admin) — 이메일만으로 정지(신고 접수함에서 대상 id 없이 처리할 때). Req: `email, status`

관리자 권한 판정은 매 요청마다 user-service DB에서 role을 다시 확인합니다(JWT의 role 클레임은 발급 시점 값이라 신뢰하지 않음).

---

## 16. 신고 접수 — post-service

> Gateway 외부 경로 `/api/reports/**`
>
- `POST /api/reports` (Bearer) — 신고 접수. Req: 대상, 사유, 상세 내용(선택)

---

## 17. 내부 API — 서비스 간 호출 전용

외부 사용자에게 공개하지 않는 서비스 간 API. `X-Internal-Service-Key` 헤더로 검증(API Gateway가 외부에서 들어온 이 헤더는 제거).

### user-service (`/api/internal/users`)

- `GET /by-id/{userId}` — 사용자 ID → `{id, nickname}`
- `GET /by-id?id={userId}` — 사용자 ID 기준 전체 `UserResponse`
- `GET /by-email?email=` — 이메일 기준 전체 `UserResponse`
- `POST /verify-token` — Access Token 검증. Body에 토큰 문자열. 정지 계정은 여기서 즉시 401.
- `GET /{email}/region` — 사용자 활동 지역 조회
- `GET /admin-stats` — 관리자 대시보드 개요용 (총 회원/오늘 가입자)

### post-service (`/api/internal`)

- `GET /fix-deals/{id}` — FixDeal 정보 (chat-service가 dealStatus 채울 때 사용)
- `GET /proposals/{id}` — Proposal 당사자 정보
- `GET /payments/context/{postId}` — 결제 Context(payment-service의 `prepare`가 호출)
- `POST /notifications/chat-message` — 채팅 메시지 알림 생성 (chat-service가 호출)

### chat-service (`/api/internal/chat-rooms`, `/api/internal/realtime`)

- `GET /{roomId}` — 채팅방 내부 정보
- `PUT /proposals/ensure` — Proposal Context로 채팅방 생성/반환 (멱등)
- `GET /proposals/{proposalId}` — Proposal 기준 채팅방 조회
- `POST /by-fix-deal-ids` — FixDeal id 목록 → ChatRoom id 배치 조회
- `GET /exists-by-proposal/{proposalId}` — 존재 여부
- `POST /attach-deal` / `POST /detach-deal` — 채택/취소 시 FixDeal 연결·해제
- `GET /{roomId}/messages/text` — AI 계약 초안용 TEXT 메시지 조회
- `POST /api/internal/realtime/notifications` — 알림 실시간 푸시(STOMP 브로커가 chat-service에만 있어서, 채팅이 아닌 알림도 이 경로를 거침)

### payment-service (`/internal/payments`)

- `GET /post/{postId}` — post-service가 거래 완료 처리 등에 쓰는 결제 상태 조회
- `GET /admin/summary`, `GET /admin/list` — 관리자 현황판용 (post-service가 대신 호출)
- `POST /post/{postId}/settle` — 정산 확정 (계약 `accept` 처리 중 post-service가 호출)

---

# 서비스별 API 소유권 요약

```
user-service
├─ /api/auth/**
├─ /api/users/**
├─ /oauth2/**, /login/oauth2/**
├─ /api/admin/users/**
└─ /api/internal/users/**

post-service
├─ /posts/**
├─ /fix-deals/**  (읽기 전용)
├─ /reviews/**
├─ /resumes/**
├─ /profile/**
├─ /notifications/**
├─ /api/reports/**
├─ /api/ai/**
├─ /api/admin/posts/**, /reports/**, /deals/**, /overview/**, /payments/**
├─ /api/chat-rooms/proposals/**, /api/chat-rooms/fix-deals/**, /api/chat-rooms/{roomId}/detail
├─ /api/chat-rooms/{roomId}/contract/**
└─ /api/internal/** (fix-deals, proposals, payments/context, notifications)

chat-service
├─ /api/chat-rooms  (목록)
├─ /api/chat-rooms/{roomId}/messages/**
├─ /api/chat-rooms/{roomId}/counterpart, /session
├─ /api/chat-rooms/{roomId}/attachments/**
├─ /ws/chat
└─ /api/internal/chat-rooms/**, /api/internal/realtime/**

payment-service
├─ /payments/prepare, /payments, /payments/{paymentId}, /payments/post/{postId}, /payments/mine
├─ /payments/webhook
└─ /internal/payments/**
```
