## 회고

| Sprint | Keep | Problem·학습 | Try | 영역 |
| --- | --- | --- | --- | --- |
| Sprint 1 | 회원가입, 지역 설정, 수리 요청, 제안, 채택까지 핵심 사용자 흐름을 빠르게 연결한 점을 유지한다. Story와 Task를 분리하고 기능별 Acceptance Criteria와 Test를 함께 정의한 방식도 유지한다. | 정상 흐름 구현에 집중하면서 미채택 알림, 거래 완료 주체, 취소·분쟁 등 실제 거래에서 필요한 예외 정책이 뒤늦게 발견되었다. 일부 기능은 개발 환경에서는 동작했지만 배포 환경까지 검증되지 않아 실제 Increment 완료 여부를 판단하기 어려웠다. | Story 작성 시 정상 흐름뿐 아니라 권한, 상태 전이, Alternate / Exception Flow를 함께 정의한다. Done 판단 시 코드 완료만 보지 않고 Test 통과와 배포 환경 시나리오 검증까지 확인한다. | 프로세스 / 완료 기준 |
| Sprint 2 | 제안 채택 이후 채팅방 개설, 거래 시작, 수리 진행, 완료 신청, 결제까지 기능을 개별 CRUD가 아니라 하나의 거래 흐름으로 연결한 방식을 유지한다. 상태별 권한 검증과 잘못된 상태 전이를 Test로 검증한 점도 유지한다. | 거래 상태가 늘어나면서 `MATCHED`, `PRODUCT_SENT`, `REPAIRING`, `REPAIR_DONE` 등 상태 간 의미와 다음 행동을 이해하기 어려워졌다. 결제 상태와 거래 상태도 서로 영향을 주기 시작해 한쪽만 변경될 경우 데이터 불일치 가능성이 커졌다. | 거래 상태 전이 규칙을 한 곳에서 명확히 관리하고 상태별 가능한 Action을 문서·Test와 일치시킨다. 거래 상태와 결제 상태를 분리하고 화면에서는 사용자가 현재 단계와 다음 행동을 확인할 수 있도록 Timeline 형태로 표현한다. | 설계 / 프로세스 / 품질 |
| Sprint 3 | 계약 서명, 결제 상태, 수리 진행, 완료 수락, 정산까지 여러 도메인의 조건을 연결하고 실패 시 롤백 및 중복 정산 방지까지 고려한 점을 유지한다. 후기와 거래 이력을 추가해 단일 거래 이후의 사용자 경험까지 확장한 점도 유지한다. | 기능 범위가 빠르게 확대되면서 Closed Issue와 실제 Done 상태가 일치하지 않는 문제가 발생했다. 특히 AI 게시글 작성과 AI 계약서 초안 기능은 Issue는 종료됐지만 E2E Test와 Demo Evidence가 완성되지 않아 Increment 완료 여부가 불명확했다. 또한 Post, Payment, User 등 서비스 간 호출과 거래 상태가 복잡해지면서 변경 영향 범위가 커졌다. | Issue를 닫기 전에 공통 Definition of Done 체크를 필수로 수행하고 Test·Demo Evidence가 없는 기능은 Done으로 처리하지 않는다. 서비스 간 계약 변경 시 관련 Consumer와 상태 전이 Test를 함께 실행한다. AI 기능은 AI 응답 생성 성공이 아니라 실제 사용자 흐름의 시작부터 저장·등록까지 E2E 기준으로 검증한다. | 완료 기준 / 협업 / 테스트 |

## 개선 Action 추적

| 개선 Action | 담당 | 완료 조건 | 적용 시점 | Evidence | 결과 |
| --- | --- | --- | --- | --- | --- |
| Story 작성 시 정상·대안·예외 흐름을 함께 정의한다. | 각 Story 담당자 | 신규 Story에 Actor, 사전조건, 정상 Flow, Alternate / Exception Flow, 상태 변화, 권한 조건이 포함된다. | Sprint 2부터 지속 | GitHub Story / 요구사항 문서 | 적용 |
| 배포 환경 검증을 Definition of Done에 포함한다. | 기능 담당자 / 배포 담당자 | 핵심 사용자 시나리오를 배포 환경에서 실행하고 결과 또는 화면 Evidence를 남긴다. | Sprint 2부터 지속 | CI/CD 결과 / Demo Evidence | 적용 |
| 거래 상태 전이 규칙을 명시적으로 관리한다. | Backend | 각 상태별 허용 Action, 실행 주체, 다음 상태, 실패 조건이 문서와 Test에서 일치한다. | Sprint 2~3 | 요구사항 / 상태 전이 Test | 적용 |
| 거래 상태와 결제·정산 상태를 분리한다. | Backend | 거래 진행 상태와 금전 상태가 별도 필드 또는 모델로 관리되고 UI에서도 구분되어 표시된다. | 다음 Sprint | ERD / API / 상태 Timeline | PLANNED |
| 거래 진행 Timeline UI를 추가한다. | Frontend | 사용자가 현재 거래 단계와 다음 행동, 결제·정산 상태를 한 화면에서 확인할 수 있다. | 다음 Sprint | Demo / 화면 캡처 | PLANNED |
| Issue Close 전에 Definition of Done 체크를 수행한다. | 각 Issue 담당자 | Test, 배포, Demo Evidence, 문서 정합성 중 미완료 항목이 있으면 Issue를 Done으로 처리하지 않는다. | 다음 Sprint부터 | GitHub Issue 체크리스트 | PLANNED |
| 핵심 거래 흐름 E2E Test를 보강한다. | Backend / QA 담당 | `요청 → 제안 → 채택 → 계약 → 결제 → 수리 → 완료 → 정산`의 정상 흐름과 주요 실패 흐름을 반복 가능한 Test로 검증한다. | 다음 Sprint | Integration / Acceptance Test 결과 | PLANNED |
| AI 기능의 완료 기준을 사용자 종단 흐름 기준으로 변경한다. | AI 기능 담당자 | AI 응답 생성뿐 아니라 입력 유지, 사용자 수정, 실제 게시글 등록 또는 계약 저장·서명까지 검증한다. | 다음 Sprint | AI E2E Test / Demo Evidence | PLANNED |

다음 Sprint에서는 개선 Action을 너무 많이 동시에 진행하지 않고 아래 두 항목을 우선 적용한다.

1. **Issue Close 전에 Definition of Done 체크를 수행한다.**
    
    Test, 배포 환경 확인, Demo Evidence가 없으면 Issue 상태가 Closed여도 Sprint Review의 Done Increment에는 포함하지 않는다.
    
2. **거래 상태·결제 상태를 분리하고 Timeline으로 표현한다.**
    
    거래의 `MATCHED → REPAIRING → REPAIR_DONE → COMPLETED` 흐름과 `결제 대기 → 결제 완료 → 정산 대기 → 정산 완료` 흐름을 구분하여 관리하고, 사용자가 현재 상태와 다음 행동을 확인할 수 있도록 한다.
