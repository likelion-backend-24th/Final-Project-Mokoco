# 서비스별 DB 마이그레이션

user-service, post-service, payment-service가 각각 `user_db`, `post_db`, `payment_db`와
`src/main/resources/db/migration`을 소유한다. chat-service는 `post_db`를 공유하지만 Flyway는
비활성화하며, 채팅 테이블 migration도 post-service가 단독으로 소유한다.
서비스 시작 시 Flyway가 먼저 실행되고 Hibernate `ddl-auto: validate`가 엔티티 호환성을 확인한다.
DB 간 외래 키는 만들지 않는다.

## 신규 DB

로컬 기본 JDBC URL은 `createDatabaseIfNotExist=true`를 사용한다. 접속 계정에 CREATE 권한이 있으면
없는 DB를 먼저 생성하고 Flyway가 V1부터 최신 버전까지 적용한다.
운영에서 DB 생성 권한이 없는 계정을 사용할 때는 관리자가 DB를 먼저 생성해야 한다.
환경변수 등으로 JDBC URL을 재정의하면 해당 URL에도 옵션을 넣거나 DB를 미리 생성한다.
각 DB에 `flyway_schema_history`가 생성된다. 이미 적용된 SQL은 수정하지 않고,
변경사항은 해당 서비스의 `V2__설명.sql`, `V3__설명.sql`로 추가한다.
MySQL DDL은 완전한 트랜잭션 롤백을 지원하지 않으므로 실패 시 실제 테이블 상태를 먼저 확인한다.

## 기존 production DB 최초 도입

운영 DB는 Hibernate `ddl-auto=update`로 이미 생성되어 있으므로 V1부터 다시 실행하지 않는다.
애플리케이션 기본값은 항상 `baseline-on-migrate: false`다. 최초 편입 배포에서만 Compose의
서비스별 환경변수를 true로 설정한다.

| DB | baseline version | baseline 다음 실행 |
|---|---:|---|
| `user_db` | 2 | V3 legacy 호환 migration |
| `post_db` | 5 | V6 legacy 호환 migration |
| `payment_db` | 3 | V4 legacy 호환 migration |

legacy 호환 migration은 release 스키마를 현재 스키마로 올린다. 신규 빈 DB에서는 V1부터 모두
실행된 후 마지막 migration이 무변경으로 끝난다. post/payment의 이메일→사용자 ID 변환은 같은
MySQL 인스턴스의 `user_db.users`를 기준으로 하며, 매핑되지 않은 이메일이나 중복 소유자가 있으면
임의 데이터를 만들지 않고 migration을 실패시킨다.

1. release merge 전에 모든 백엔드를 중지하고 `user_db`, `post_db`, `payment_db`를 백업한다.
2. `flyway_schema_history`가 없는지 확인하고 복제 DB에서 전체 절차를 먼저 실행한다.
3. 아래 사전 조회가 0건인지 확인한다. 결과가 있으면 실제 사용자와 중복 거래를 먼저 정리한다.

   ```sql
   SELECT p.author_email FROM post_db.posts p LEFT JOIN user_db.users u ON BINARY p.author_email=BINARY u.email WHERE u.id IS NULL;
   SELECT p.repairer_email FROM post_db.proposals p LEFT JOIN user_db.users u ON BINARY p.repairer_email=BINARY u.email WHERE u.id IS NULL;
   SELECT post_id, COUNT(*) FROM post_db.fix_deals WHERE status <> 'CANCELED' GROUP BY post_id HAVING COUNT(*) > 1;
   SELECT proposal_id, COUNT(*) FROM post_db.fix_deals WHERE status <> 'CANCELED' GROUP BY proposal_id HAVING COUNT(*) > 1;
   SELECT post_id, COUNT(*) FROM post_db.proposals WHERE is_adopted=TRUE GROUP BY post_id HAVING COUNT(*) > 1;
   ```

4. EC2의 `/home/ubuntu/mokoco/.env`에 최초 배포용 값을 넣고 migration 소유 서비스만 먼저 기동한다.

   ```dotenv
   USER_FLYWAY_BASELINE_ON_MIGRATE=true
   POST_FLYWAY_BASELINE_ON_MIGRATE=true
   PAYMENT_FLYWAY_BASELINE_ON_MIGRATE=true
   ```

   ```bash
   docker compose up -d --build user-service post-service payment-service
   docker compose logs user-service post-service payment-service
   ```

5. 각 history에 BASELINE과 바로 다음 migration만 기록됐는지 확인한다. V1~baseline 버전의
   `type='SQL'` 행이 있으면 중단한다.

   ```sql
   SELECT installed_rank, version, description, type, success FROM user_db.flyway_schema_history ORDER BY installed_rank;
   SELECT installed_rank, version, description, type, success FROM post_db.flyway_schema_history ORDER BY installed_rank;
   SELECT installed_rank, version, description, type, success FROM payment_db.flyway_schema_history ORDER BY installed_rank;
   ```

6. 세 서비스의 Flyway와 Hibernate validation 성공 후 chat-service를 기동한다.
7. `.env`의 세 값을 삭제하고 서비스를 재생성해 기본값 false로 복귀시킨다.

   ```bash
   docker compose up -d --force-recreate user-service post-service payment-service
   docker compose up -d chat-service api-gateway frontend
   ```

baseline은 기존 스키마를 고치지 않는다. 환경변수를 true로 설정하기 전에 백업·사전 조회·복제 DB
검증이 모두 끝나야 한다. 이미 history가 있는 DB에는 새 baseline을 만들지 말고 현재 이력부터
확인한다.

post DB의 채팅 데이터는 다른 DB로 옮기지 않는다. V6는 `chat_room`을 `chat_rooms`로 바꾸더라도
방/메시지 ID와 `chat_room_id` 참조를 보존한다.
기존 `scripts/*.sql` 및 `db/manual/proposal-chat.sql`은 과거 스키마용 수동 스크립트이며
Flyway 자동 실행 대상이 아니다. V1로 생성한 DB에 재실행하지 않는다.

## Testcontainers + MySQL 검증

Docker 실행 상태에서 각 서비스 디렉터리에서 실행한다:

```powershell
.\gradlew.bat :test --tests '*FlywaySchemaTest' --rerun-tasks
```

네 서비스 모두 JUnit Testcontainers가 `mysql:8.0` 컨테이너를 생성하고,
임의 포트와 테스트 계정으로 연결한 뒤 종료 시 정리한다.
`FLYWAY_TEST_URL`이나 수동 DB 생성은 필요 없다. 기존 DB에 연결하지 않는다.
Docker가 없으면 테스트를 건너뛰지 않고 실패하므로 CI에서도 Docker가 필요하다.
최초 실행에는 MySQL 및 Testcontainers 보조 이미지 다운로드가 필요할 수 있다.

검증 범위는 V1 적용, JPA 스키마 호환, checksum 검증, 재실행 시 추가 적용 0건,
기본 자동 baseline/clean 방지 설정이다. post-service의 `FlywayUpgradeTest`는 legacy production
형태의 테이블과 데이터를 만든 뒤 V5 baseline→V6 적용 시 V1~V5가 실행되지 않고 ID와 채팅
데이터가 보존되는 경로도 검증한다. H2 기반 테스트는 기존대로 유지한다.
이 테스트는 채택 동시성이나 서비스 간 전체 흐름을 검증하지 않는다.

## 채택·거래 무결성

release production과 release V1에 이미 반영된 unique 컬럼·제약을 중복 생성하지 않도록
`V2__active_deal_uniqueness.sql`은 제거했다. post-service V6가 컬럼과 제약이 없는 신규/legacy
DB에서만 생성한다. CANCELED 이력은 여러 개 보존하고 COMPLETED 거래는 중복 생성을 차단한다.
기존 중복 데이터가 있으면 V6는 실패하며 데이터를 임의로 삭제하거나 취소하지 않는다.

```sql
SELECT post_id, COUNT(*) FROM fix_deals WHERE status <> 'CANCELED' GROUP BY post_id HAVING COUNT(*) > 1;
SELECT proposal_id, COUNT(*) FROM fix_deals WHERE status <> 'CANCELED' GROUP BY proposal_id HAVING COUNT(*) > 1;
SELECT post_id, COUNT(*) FROM proposals WHERE is_adopted = TRUE GROUP BY post_id HAVING COUNT(*) > 1;
```

채택/취소는 Post → Proposal → FixDeal 순서로 잠근다. 거래 상태 변경은 FixDeal 행을 잠그고
최신 상태를 검사한다. 취소는 MATCHED에서만 가능하며 취소·완료된 거래는 재개하지 않는다.
동일 상태 변경 요청은 권한 확인 후 성공으로 처리하며 완료 시각을 다시 쓰지 않는다.
취소 후 재채택은 새 FixDeal을 만들고 기존 Proposal 상담방을 재사용한다.

허용 상태 전이:
- 배송 경로: MATCHED → PRODUCT_SENT → REPAIRING → REPAIR_DONE → COMPLETED
- 기존 계약 경로: 양측 서명 후 MATCHED → REPAIRING → REPAIR_DONE → COMPLETED
- 취소: MATCHED → CANCELED

검증 명령(post-service 디렉터리):
```powershell
.\gradlew.bat :test --tests '*ProposalConcurrencyTest' --tests '*FlywayUpgradeTest' --tests '*FixDeal*Test'
```
채택 경쟁·동일 채택 재시도·취소/배송 경합·완료 재시도·DB UNIQUE·V1 이력 보존 업그레이드를
MySQL에서 검증한다. 외부 User/Payment/Chat API는 이 테스트에서 대역을 사용한다.
chat-service의 `ChatRoomConcurrencyTest`는 실제 MySQL에서 8개 동시 생성 요청의 동일 방 반환,
UNIQUE, 상담방 재사용을 검증한다. 분산 트랜잭션이나 동기화 메시지 순서 보장은 별도 범위다.

## payment-service V2

`V2__payment_orders.sql`은 서버가 발급한 결제 주문과 견적/수취인 스냅샷을 저장한다.
[인증 및 결제 흐름](SECURITY-PAYMENT.md)의 동시 배포 및 기존 미확정 결제 처리 사항을 확인한다.
