# userId 기반 인증과 소유권

## General
JWT Subject뿐 아니라 애플리케이션 principal과 데이터 소유권을 변경 불가능한 userId로 통일한다. 이메일은 user-service의 로그인·연락처 속성이며 다른 서비스의 소유권 키로 사용하지 않는다.

## Function
- 공통 principal: LoginUser(userId, role). 역할은 user-service가 검증한 사용자 응답으로 전달하며 누락된 역할은 인증 실패로 처리한다.
- 내 정보·지역·게시글·견적·후기·이력서·알림·결제 서비스 인자는 사용자 ID를 받는다.
- 공개 이력서·후기·거래 내역 조회도 사용자 ID를 받는다. 프론트의 ID 비교는 표시 제어이며 권한 검사는 서버에서 다시 수행한다.

## Spec: 배포와 기존 데이터 이관
1. 쓰기 요청을 중지하고 각 DB를 백업한다. V1/V2를 수정하거나 이미 사용한 마이그레이션을 다시 실행하지 않는다.
2. user-service DB의 `SELECT id AS user_id, email FROM users` 결과를 UTF-8 CSV(`user_id,email` 헤더)로 내보낸다. 과거 이메일이 변경·재사용됐거나 탈퇴 사용자의 레코드가 남았다면 운영 기록으로 기존 소유자를 확정해야 한다. 현재 이메일 매칭만으로 추정하지 않는다.
3. `python backend/tools/identity-map.py users.csv` 출력 SQL을 파일로 저장하고 post/payment DB 각각에 적용한다. SQL/CSV에는 개인정보가 있으므로 Git에 넣지 않는다. ID와 이메일의 매핑이 잘못된 경우 자동 덮어쓰지 않고 작업을 중단한다.
4. user-service V2, post/payment V3를 적용한다. 매핑 누락·중복 또는 기존 payment_orders payerId 불일치는 중단한다. 기존 이메일 열은 NULL 허용 이력 열로 보존하고 새 레코드와 소유권 비교는 ID 열만 사용한다. 비어 있는 신규 DB에는 매핑 데이터가 필요 없다.
5. user-service와 나머지 서비스 및 프론트를 함께 전환한다. `/resumes/{userId}`, `/profile/{userId}/transactions`, `/reviews?revieweeId=...`와 결제 응답의 payerId/payeeId가 새 계약이다. 이전 클라이언트와 혼용하지 않는다.
6. 이관 후 매핑 테이블과 파일은 보관 정책에 따라 접근을 제한한다. MySQL DDL은 자동 커밋되므로 실패 시 Flyway repair만으로 강행하지 말고 백업과 실제 적용 상태를 확인한다.

새 DB 마이그레이션·테스트·빌드·운영 적용은 이 작업에서 실행하지 않았다.
