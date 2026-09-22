# ERD 정의서

## 관계 원칙

- Foreign Key는 같은 Service DB 안에서만 사용합니다.
- 다른 Service ID는 논리 참조입니다.
- 생성 시점 값이 필요하면 Snapshot 목적과 갱신 금지를 명시합니다.
- 상태 전이와 Unique·Transaction 근거를 업무 규칙과 실제 Test에 연결합니다.

1. user-service ERD

```mermaid
erDiagram
    User {
        bigint id PK
        varchar email UK
        varchar password
        varchar name
        varchar nickname UK
        varchar role "enum: USER, ADMIN"
        bigint region_id FK "nullable"
        datetime createdAt
        datetime updatedAt
    }

    SocialAccount {
        bigint id PK
        bigint user_id FK
        varchar provider "enum: GOOGLE, KAKAO"
        varchar providerId "(provider + providerId) UK"
    }

    Region {
        bigint id PK
        varchar regionCode
        varchar sido
        varchar sigungu
        varchar dong
    }

    RefreshToken {
        bigint id PK
        bigint user_id FK,UK
        varchar token
    }

    User ||--o{ SocialAccount : "has"
    Region ||--o{ User : "assigned to"
    User ||--o| RefreshToken : "has"
```

1. post-service ERD

```mermaid
erDiagram
    %% User는 user-service 소유
    %% Post Service에서는 User.id / User.email을 논리 참조하며 실제 FK는 사용하지 않음

    User {
        bigint id PK
        varchar email UK
    }

    Post {
        bigint id PK
        varchar title
        text content
        varchar authorEmail "User.email 논리 참조"
        varchar regionName
        varchar regionCode
        boolean publiclyVisible
        varchar category "enum: PostCategory"
        varchar status "enum: WAITING, MATCHED, COMPLETED"
        datetime createdAt
        datetime updatedAt
    }

    PostImage {
        bigint id PK
        bigint post_id FK
        varchar imageUrl
        varchar storedFileName
        int sortOrder
        datetime createdAt
    }

    Proposal {
        bigint id PK
        bigint post_id FK
        varchar repairerEmail "User.email 논리 참조"
        int estimatedPrice
        text content
        boolean isAdopted
        boolean attachResume
    }

    FixDeal {
        bigint id PK
        bigint postId "Post.id"
        bigint proposalId "Proposal.id"
        bigint requesterId "User.id 논리 참조"
        bigint repairerId "User.id 논리 참조"
        varchar status "enum: FixDealStatus"
        datetime createdAt
        datetime completedAt
    }

    RepairContract {
        bigint id PK
        bigint chatRoomId "Chat Service ChatRoom.id 논리 참조"
        int revision "(chatRoomId+revision) UK"
        bigint authorId "User.id 논리 참조"
        longtext termsJson
        varchar documentHash
        varchar status "DRAFT/SIGNING/SIGNED/SUPERSEDED"
        datetime createdAt
        datetime requestedAt
        datetime signedAt
    }

    ContractSignature {
        bigint id PK
        bigint contract_id FK
        bigint signerId "User.id 논리 참조"
        varchar signerName
        varchar documentHash
        varchar consentText
        datetime signedAt
    }

    Review {
        bigint id PK
        bigint postId UK "Post.id"
        varchar reviewerEmail "User.email 논리 참조"
        varchar revieweeEmail "User.email 논리 참조"
        int rating
        varchar content
        datetime createdAt
    }

    ReviewImage {
        bigint id PK
        bigint review_id FK
        varchar imageUrl
        varchar storedFileName
        int sortOrder
        datetime createdAt
    }

    Report {
        bigint id PK
        varchar targetType "enum: POST, USER"
        bigint targetId "Post.id 또는 외부 User.id 논리 참조"
        varchar targetEmail "User.email 논리 참조"
        varchar reporterEmail "User.email 논리 참조"
        varchar reason "enum"
        text detail
        varchar status "enum: PENDING, RESOLVED, DISMISSED"
        datetime createdAt
    }

    Resume {
        bigint id PK
        varchar userEmail UK "User.email 논리 참조"
        varchar headline
        varchar introduction
        datetime createdAt
        datetime updatedAt
    }

    ResumeCareer {
        bigint id PK
        bigint resume_id FK
        varchar period
        varchar description
        int sortOrder
    }

    ResumeSkill {
        bigint resume_id FK
        varchar skillName
        int sortOrder
    }

    Notification {
        bigint id PK
        varchar recipientEmail "User.email 논리 참조"
        varchar type "enum"
        bigint postId "nullable"
        bigint proposalId "nullable"
        bigint chatRoomId "Chat Service ChatRoom.id 논리 참조, nullable"
        varchar message
        boolean isRead
        datetime createdAt
    }

    NotificationSetting {
        bigint id PK
        varchar userEmail UK "User.email 논리 참조"
        boolean proposalReceived
        boolean proposalAdopted
        boolean chatMessage
    }

    User ||--o{ Post : "writes"
    Post ||--o{ PostImage : "has"
    Post ||--o{ Proposal : "receives"

    Post ||--o| FixDeal : "results in"
    Proposal ||--o| FixDeal : "adopted into"

    User ||--o{ FixDeal : "requests"
    User ||--o{ FixDeal : "repairs"

    RepairContract ||--o{ ContractSignature : "signed by"
    User ||--o{ ContractSignature : "signs"

    Post ||--o| Review : "gets"
    User ||--o{ Review : "writes"
    User ||--o{ Review : "receives"
    Review ||--o{ ReviewImage : "has"

    User ||--o{ Report : "reports"
    Post ||--o{ Report : "is reported"

    User ||--o| Resume : "has"
    Resume ||--o{ ResumeCareer : "has"
    Resume ||--o{ ResumeSkill : "has"

    User ||--o{ Notification : "receives"
    User ||--o| NotificationSetting : "has"
```

1. payment-service ERD

```mermaid
erDiagram
    %% payment-service 소유
    %% 외부 서비스 User/Post/FixDeal은 ID로만 논리 참조
    %% PaymentOrder.paymentId와 Payment.portonePaymentId는
    %% 동일 결제 건을 식별하지만 DB FK는 생성하지 않는다.

    User {
        bigint id PK
    }

    Post {
        bigint id PK
    }

    FixDeal {
        bigint id PK
    }

    PaymentOrder {
        varchar paymentId PK
        bigint postId UK
        bigint fixDealId
        bigint payerId
        bigint payeeId
        int baseAmount
        int totalAmount
        datetime createdAt
    }

    Payment {
        bigint id PK
        bigint postId UK
        varchar portonePaymentId UK
        bigint payerId
        bigint payeeId
        int amount
        int feeAmount
        int netAmount
        varchar status "COMPLETED, FAILED"
        datetime createdAt
        datetime paidAt
    }

    User ||--o{ PaymentOrder : "payerId"
    User ||--o{ PaymentOrder : "payeeId"
    User ||--o{ Payment : "payerId"
    User ||--o{ Payment : "payeeId"

    Post ||--o| PaymentOrder : "postId"
    FixDeal ||--o| PaymentOrder : "fixDealId"
    Post ||--o| Payment : "postId"

    PaymentOrder ||--o| Payment : "paymentId = portonePaymentId"
```

1. chat-service

```mermaid
erDiagram
    %% Chat Service는 ChatRoom / ChatMessage만 소유
    %% User, Post, Proposal, FixDeal은 타 서비스 데이터이므로 ID만 논리 참조
    %% 실제 FK 관계를 만들지 않음

    User {
        bigint id PK
    }

    Post {
        bigint id PK
    }

    Proposal {
        bigint id PK
    }

    FixDeal {
        bigint id PK
    }

    ChatRoom {
        bigint id PK
        bigint fixDealId UK "Post Service FixDeal.id 논리 참조, nullable"
        bigint proposalId UK "Post Service Proposal.id 논리 참조"
        bigint requesterId "User Service User.id 논리 참조"
        bigint repairerId "User Service User.id 논리 참조"
        bigint postId "Post Service Post.id 논리 참조"
        varchar postTitle "생성 시점 snapshot"
        varchar status "enum: OPEN, CLOSED"
        datetime createdAt
        datetime closedAt
    }

    ChatMessage {
        bigint id PK
        bigint chat_room_id FK
        bigint senderId "User Service User.id 논리 참조"
        text content
        varchar messageType "enum: TEXT, IMAGE, VIDEO, FILE 등"
        datetime createdAt
        datetime readAt
        varchar attachmentKey
        varchar attachmentName
        varchar attachmentMime
        bigint attachmentSize
        datetime deletedAt
    }

    Proposal ||--o| ChatRoom : "opens by proposalId"
    FixDeal ||--o| ChatRoom : "attaches by fixDealId"
    Post ||--o{ ChatRoom : "references postId"

    User ||--o{ ChatRoom : "requesterId"
    User ||--o{ ChatRoom : "repairerId"

    ChatRoom ||--o{ ChatMessage : "contains"
    User ||--o{ ChatMessage : "senderId"
```
