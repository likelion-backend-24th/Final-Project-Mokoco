# API·통합 계약

### 공통 규약

- 외부(브라우저) 호출 Prefix: `/api/*`. 내부 서비스 간 호출: `/internal/*` (외부 미공개)
- 인증 방식이 두 가지 혼재합니다 — 마이그레이션 미완료 상태로, 신규 기능은 `Authorization: Bearer {accessToken}`로 통일 중
    - 레거시: `X-User-Email` 헤더 (BFF가 쿠키의 email을 그대로 실어 보냄, 서버 서명 검증 없음)
    - 신규: `Authorization: Bearer {accessToken}` (JWT, post-service가 user-service에 검증 요청)
- 실패 응답은 `{"error": "메시지"}` 형태
- 인증 없음/무효 401, 권한 부족 403, 리소스 없음 404, 상태 충돌 409
- 브라우저는 직접 백엔드를 호출하지 않고 대부분 Next.js BFF(`/api/*` route handler)를 거쳐 쿠키의 토큰을 헤더로 주입받습니다. 일부(멀티파트 업로드, 결제 웹훅, WebSocket)는 Caddy가 게이트웨이를 건너뛰고 해당 서비스로 직결합니다.

---

# Mokoco API 명세

## 1. 인증 — user-service

- `POST /api/auth/signup` — 회원가입

    Req: `email, password, name, nickname, regionCode(선택)`

    - 비밀번호: 8자 이상, 영문·숫자 포함
    - 성공 시 생성된 `userId` 반환
    - 현재 Controller 기준 성공 상태는 `200 OK`
    - 이메일/닉네임 중복 시 오류
- `POST /api/auth/signin` — 로그인

    Req: `email, password`

    - 성공 시 Access Token / Refresh Token 반환
    - JWT Subject는 `email`이 아니라 `userId`
- `POST /api/auth/reissue` — Access Token 재발급

    Req: `refreshToken`

    - 기존 클라이언트 호환을 위해 `email` 필드가 DTO에 남아 있지만 인증에는 사용하지 않음
    - Refresh Token 내부의 `userId`를 기준으로 사용자 식별
- `/oauth2/authorization/{provider}` → `/login/oauth2/code/{provider}` — 소셜 로그인
    - Spring Security OAuth2 표준 플로우
    - 지원 Provider: Google, Kakao

---

## 2. 내 정보 / 활동 지역 — user-service

- `GET /api/users/me` — 내 정보 조회

    반환 주요 필드:

    - `id`
    - `email`
    - `name`
    - `nickname`
    - `role`
    - `regionCode`
    - `regionName`

> 현재 `User` 엔티티와 `UserResponse`에는 `status` 필드가 없음.
>
- `PATCH /api/users/me` — 이름/닉네임 수정

    Req: `name, nickname`

- `PATCH /api/users/me/password` — 비밀번호 변경

    Req: `currentPassword, newPassword`

- `GET /api/users/me/region` — 내 활동 지역 조회
    - 지역 미설정 시 `404`
- `PATCH /api/users/me/region` — 활동 지역 생성/변경
    - 사용자는 하나의 `Region`을 참조
    - 기존 지역이 있으면 변경
    - 인증 사용자 식별은 `userId` 기준

---

## 3. 게시글 — post-service

> Gateway 기준 외부 경로는 `/posts/**`
>
- `POST /posts` — 게시글 작성

    `multipart/form-data`

    Req:

    - part `post`
        - `title`
        - `content`
        - `category`
    - part `images` — 선택, 여러 장 가능

    가격은 게시글에 저장하지 않고 Proposal 단계에서 결정.

- `GET /posts` — 게시글 목록 조회

    Query:

    - `category` — 선택
    - `regionScope` — `SIDO | SIGUNGU | DONG`
    - 기본값: `SIDO`
    - `page` — 기본 0
    - `size` — 기본 20

> 현재 `RegionScope`에는 `ALL`이 존재하지 않음.
>
- `GET /posts/{id}` — 게시글 상세 조회
- `PATCH /posts/{id}` — 게시글 수정
    - 작성자만 가능
    - Req: `title, content, category`
- `DELETE /posts/{id}` — 게시글 삭제
    - 작성자만 가능
- `PATCH /posts/{id}/visibility` — 공개 여부 변경

    Req:

    - `publiclyVisible`
- `POST /posts/{id}/images` — 이미지 추가

    `multipart/form-data`

- `DELETE /posts/{id}/images/{imageId}` — 이미지 삭제

### PostStatus

```
WAITING
→ MATCHED
→ COMPLETED
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

- `POST /posts/{postId}/proposals` — 제안 등록

    Req:

    - `estimatedPrice`
    - `content`
    - `attachResume`

    인증 사용자는 `LoginUser.userId`로 식별.

- `GET /posts/{postId}/proposals` — 제안 목록 조회
- `PATCH /posts/{postId}/proposals/{proposalId}/adopt` — 제안 채택
    - 게시글 작성자만 가능
    - 제안 채택
    - `PostStatus → MATCHED`
    - `FixDeal` 생성
    - 중복/동시 채택은 비관적 락 및 DB 제약조건으로 제어
- `PATCH /posts/{postId}/proposals/{proposalId}/cancel` — 채택 취소
    - 현재 실제 Controller에 존재
    - 취소 가능한 거래 상태인지 검증
- `DELETE /posts/{postId}/proposals/{proposalId}` — 제안 삭제
    - 제안 작성자 본인만 가능

> 제안 수정 API는 현재 없음.
>

---

## 5. 거래 — FixDeal / post-service

### 거래 조회

- `GET /fix-deals/{fixDealId}` — 거래 상세 조회
    - 거래 참여자만 접근
- `GET /posts/{postId}/fix-deal` — 게시글 기준 FixDeal 상태 조회

### 기존 직접 상태 전이 API

현재 코드에는 다음 API도 여전히 존재한다.

- `PATCH /fix-deals/{fixDealId}/product-sent`
- `PATCH /fix-deals/{fixDealId}/repairing`
- `PATCH /fix-deals/{fixDealId}/repair-done`
- `PATCH /fix-deals/{fixDealId}/complete`

따라서 현재 구현을 기준으로 하면 FixDeal API를 **읽기 전용이라고 표현하면 안 됨**.

### FixDealStatus

```
MATCHED
→ PRODUCT_SENT
→ REPAIRING
→ REPAIR_DONE
→ COMPLETED

CANCELED
```

`PRODUCT_SENT`는 현재 Enum과 Controller에 실제로 남아 있음.

---

## 6. 계약서 — post-service

계약은 ChatRoom ID를 기준으로 관리하지만 계약 데이터 자체는 `post-service`가 소유한다.

Gateway에서도 계약 경로는 post-service로 라우팅된다.

### 계약 조회

- `GET /api/chat-rooms/{roomId}/contract`

반환:

- 현재 계약 버전
- 서명 상태
- 거래/결제 관련 개요

### 계약 초안 작성

- `POST /api/chat-rooms/{roomId}/contract`

Req:

- `baseId`
- `terms`

`baseId`

- 최초 계약이면 `null`
- 기존 계약 수정 시 기준 계약 버전 ID

### 계약 상태 처리

- `POST /api/chat-rooms/{roomId}/contract/{action}`

허용 Action:

```
request
sign
start
finish
accept
```

Req 주요 필드:

- `versionId`
- `documentHash`
- `signerName`
- `consent`

처리:

- `request`
    - 서명 요청
- `sign`
    - 계약 전자 서명
- `start`
    - 거래 진행 시작
- `finish`
    - 작업 완료 요청
- `accept`
    - 의뢰자가 완료 확정

> 현재 Payment 엔티티에는 `settledAt`이 없으므로 `accept`가 "정산 확정 timestamp를 저장한다"고 명세하면 현재 코드와 맞지 않음.
>

---

## 7. 채팅방 생성/연결 — post-service orchestration

ChatRoom Entity와 ChatMessage는 `chat-service`로 분리됐지만, Proposal/FixDeal 정보를 알고 있는 post-service가 채팅방 생성에 필요한 Context를 구성하여 chat-service를 호출한다.

### 제안 기준 채팅방

- `POST /api/chat-rooms/proposals/{proposalId}`
    - 제안 기준 채팅방 생성 또는 기존 방 반환
- `GET /api/chat-rooms/proposals/{proposalId}`
    - 제안 기준 채팅방 조회

### 거래 기준 채팅방

- `POST /api/chat-rooms/fix-deals/{fixDealId}`
    - FixDeal 기준 채팅방 생성 또는 기존 채팅방에 FixDeal 연결
- `GET /api/chat-rooms/fix-deals/{fixDealId}`
    - FixDeal 기준 채팅방 조회

### 상세

- `GET /api/chat-rooms/{roomId}/detail`
    - post-service가 ChatRoom 정보와 거래 정보를 조합해서 반환

---

## 8. 채팅 / 메시지 / 첨부파일 — chat-service

### 채팅방 목록

- `GET /api/chat-rooms`

Query:

- `page` — 기본 0
- `size` — 기본 5

현재 로그인 사용자가 참여 중인 채팅방 목록 반환.

### 상대방 정보

- `GET /api/chat-rooms/{roomId}/counterpart`
    - 채팅 상대방 `id`, `nickname` 반환
- `GET /api/chat-rooms/session`
    - 현재 Access Token 기준 사용자 검증 정보 반환

### 메시지 조회

- `GET /api/chat-rooms/{roomId}/messages`

Query:

- `before` — 선택, 메시지 ID 기반 커서

### 메시지 삭제

- `DELETE /api/chat-rooms/{roomId}/messages/{messageId}`
    - 본인이 작성한 메시지만 삭제 가능
    - soft delete 방식

### WebSocket

Endpoint:

```
/ws/chat
```

STOMP Send:

```
/app/chat/{roomId}
```

Subscribe:

```
/topic/chat/{roomId}
```

메시지 송신 시 사용자 식별은 WebSocket Principal의 `userId`.

### 첨부파일

- `POST /api/chat-rooms/{roomId}/attachments`

`multipart/form-data`

Req:

- `file`

업로드 완료 시 ChatMessage를 생성하고 `/topic/chat/{roomId}`로 전파.

- `GET /api/chat-rooms/{roomId}/attachments/{messageId}`
    - 인증 및 채팅방 참여 여부 확인 후 다운로드

---

## 9. 결제 — payment-service

> Gateway 외부 경로는 `/payments/**`
>

현재 결제는 `PaymentOrder → PortOne → Payment` 구조다.

### 결제 주문 준비

- `POST /payments/prepare`

Req:

```json
{
  "postId": 1
}
```

서버가 post-service에서 결제 Context를 조회해서 다음 값을 확정:

- `postId`
- `fixDealId`
- `payerId`
- `payeeId`
- `baseAmount`
- `totalAmount`

반환 예:

- `paymentId`
- `postId`
- `baseAmount`
- `totalAmount`
- `payeeId`

`paymentId`는 서버가 생성한다.

### 결제 확인

- `POST /payments`

현재 DTO:

- `postId`
- `payeeId`
- `amount`
- `baseAmount`
- `paymentId`

인증된 `payerId`는 클라이언트 입력이 아니라 서버의 `LoginUser.userId`를 사용한다.

서버는 클라이언트 값만 신뢰하지 않고:

```
PaymentOrder
+
PortOne 결제 조회 결과
```

를 이용해 검증한다.

검증 대상:

- paymentId
- 결제 상태
- 금액
- 통화
- payer/payee
- postId

동일 결제 요청은 멱등 처리.

### 결제 조회

- `GET /payments/{paymentId}` — 결제 ID 기준 조회
- `GET /payments/post/{postId}` — Post 기준 결제 조회

### PortOne Webhook

- `POST /payments/webhook`

필수 Header:

- `webhook-id`
- `webhook-timestamp`
- `webhook-signature`

서명을 검증한 뒤 PortOne 결제 정보를 서버에서 다시 조회한다.

### 현재 없는 API

현재 최신 `dev` 기준으로 다음 API는 PaymentController에 존재하지 않는다.

```
GET /api/payments/mine
POST /api/payments/post/{postId}/settle
```

또한 현재 `Payment` 엔티티에는:

```
settledAt
```

필드가 없음.

---

## 10. 후기 — post-service

> Gateway 외부 경로 `/reviews/**`
>
- `POST /reviews`

`multipart/form-data`

Req:

- part `review`
    - `postId`
    - `rating`
    - `content`
- part `images`
    - 선택

작성자는 인증된 `userId` 기준.

- `GET /reviews`

Query:

- `revieweeId`
- `page`
- `size`

> 기존 `revieweeEmail`이 아니라 `revieweeId`.
>
- `GET /reviews/{reviewId}`
    - 후기 단건 조회
- `GET /reviews/exists?postId={postId}`
    - 해당 Post에 후기 존재 여부

현재 Controller에는 별도의:

```
GET /reviews/by-post
```

API가 없음.

---

## 11. 이력서 — post-service

> Gateway 외부 경로 `/resumes/**`
>
- `POST /resumes`
    - 내 이력서 작성
- `PATCH /resumes`
    - 내 이력서 수정
- `DELETE /resumes`
    - 내 이력서 삭제

Req:

- `headline`
- `introduction`
- `skills[]`
- `careers[]`

### 조회

- `GET /resumes/me`
    - 내 이력서 조회
- `GET /resumes/{userId}`
    - 특정 사용자 이력서 조회

> 기존 `/api/resume/{email}` 방식이 아니라 `userId` 기반.
>

---

## 12. 프로필 / 거래 내역 — post-service

> Gateway 외부 경로 `/profile/**`
>
- `GET /profile/transactions`

Query:

- `role=requester|repairer`
- `page`
- `size`

내 거래 내역 조회.

- `GET /profile/reviews`
    - 내가 작성한 후기 목록
- `GET /profile/{userId}/transactions`

Query:

- `role`
- `page`
- `size`

특정 사용자 거래 내역 조회.

> 기존 `{email}`이 아니라 `{userId}`.
>

---

## 13. 알림 — post-service

> Gateway 외부 경로 `/notifications/**`
>
- `GET /notifications`
    - 내 알림 목록
- `GET /notifications/unread-count`
    - 읽지 않은 알림 수
- `PATCH /notifications/{id}/read`
    - 단일 알림 읽음 처리
- `PATCH /notifications/read-all`
    - 전체 읽음 처리
- `GET /notifications/settings`
    - 알림 설정 조회
- `PUT /notifications/settings`
    - 알림 설정 변경

사용자 식별은 `recipientEmail`이 아니라 런타임 기준 `userId` 사용.

---

## 14. AI 작성 지원 — post-service / Gemini

### 게시글 초안

- `POST /api/ai/post-draft`

`multipart/form-data`

Req:

- `images`
- `title`
- `content`
- `category`

AI 결과는 게시글로 바로 저장하지 않고 초안만 반환.

### 계약서 AI 초안

- `POST /api/chat-rooms/{roomId}/contract/ai-draft`

Req:

- `baseId`
- `currentTerms`
- `instructions`

계약 AI는 post-service가 담당하지만 채팅 내역은 chat-service 내부 API로 조회한다.

금액 등 중요한 거래 데이터는 AI가 임의로 결정하지 않고 서버의 거래 데이터를 기준으로 보정한다.

---

## 15. 내부 API — 서비스 간 호출 전용

외부 사용자에게 공개하지 않는 서비스 간 API.

### user-service

- `GET /api/internal/users/by-id/{userId}`
    - 사용자 ID → 최소 사용자 정보/닉네임 조회
- `GET /api/internal/users/by-id?id={userId}`
    - 사용자 ID 기준 전체 UserResponse 조회
- `POST /api/internal/users/verify-token`
    - Access Token 검증
    - Token Subject의 `userId` 기준 사용자 반환
- `GET /api/internal/users/by-id/{userId}/region`
    - 사용자 활동 지역 조회

> 기존 `by-email` 기반 내부 API는 최신 Controller 기준 제거됨.
>

### chat-service

Base:

```
/internal/chat-rooms
```

- `POST /internal/chat-rooms`
    - Proposal/FixDeal Context를 받아 채팅방 생성 또는 기존 방 반환
- `GET /internal/chat-rooms/{roomId}`
    - 채팅방 내부 정보 조회
- `GET /internal/chat-rooms/proposals/{proposalId}/exists`
    - Proposal 기준 채팅방 존재 여부
- `PUT /internal/chat-rooms/proposals/sync`
    - Proposal/FixDeal 상태를 ChatRoom과 동기화
- `GET /internal/chat-rooms/{roomId}/text-messages?userId={userId}`
    - 계약 AI 등에 사용할 TEXT 메시지 조회
- `POST /internal/chat-rooms/notifications/{userId}`
    - 사용자 WebSocket 알림 전송

모든 내부 호출은 `X-Internal-Service-Key` 검증.

### payment-service

- `GET /internal/payments/post/{postId}`
    - post-service가 거래 완료 처리 등에 사용할 결제 상태 조회
    - `X-Internal-Service-Key` 필요

---

# 서비스별 API 소유권 요약

```
user-service
├─ /api/auth/**
├─ /api/users/**
├─ /oauth2/**
└─ /api/internal/users/**

post-service
├─ /posts/**
├─ /fix-deals/**
├─ /reviews/**
├─ /resumes/**
├─ /profile/**
├─ /notifications/**
├─ /api/ai/**
├─ /api/chat-rooms/proposals/**
├─ /api/chat-rooms/fix-deals/**
├─ /api/chat-rooms/{roomId}/detail
└─ /api/chat-rooms/{roomId}/contract/**

chat-service
├─ /api/chat-rooms
├─ /api/chat-rooms/{roomId}/messages/**
├─ /api/chat-rooms/{roomId}/counterpart
├─ /api/chat-rooms/{roomId}/attachments/**
├─ /ws/chat
└─ /internal/chat-rooms/**

payment-service
├─ /payments/prepare
├─ /payments
├─ /payments/{paymentId}
├─ /payments/post/{postId}
├─ /payments/webhook
└─ /internal/payments/**
```
