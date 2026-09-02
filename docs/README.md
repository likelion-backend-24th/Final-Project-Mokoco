# [양식] 문서 원본·Git Snapshot 인덱스

이 파일은 팀 Repository의 `docs/README.md`로 사용합니다. 첫날에는 아래 15종 Notion 원본 URL과 담당자만 등록하고, 15개 Markdown 본문은 만들지 않습니다. 이 인덱스는 15종 기준 문서 수에 포함하지 않으며 Git에서 직접 관리합니다.

## 원본·Snapshot 링크 목차

| 문서 | Notion 원본 URL | 담당자 | 최근 Git Snapshot(보존본) |
|---|---|---|---|
| 요구사항 | [[요구사항]](https://app.notion.com/p/3cd73873401a80418840dd22bbca3876) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/요구사항.md` 생성 |
| 공통 완료 기준 | [[공통 완료 기준]](https://app.notion.com/p/Definition_of_Done-3cd73873401a8078ae8bf0443bf128e9) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/Definition_of_Done.md` 생성 |
| 화면 설계 | [[화면 설계]](https://app.notion.com/p/3cd73873401a80a5a10afa8658598cdf) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/화면설계.md` 생성 |
| 서비스 경계 | [[서비스 경계]](https://app.notion.com/p/3cd73873401a80c998b4d9c2a481fe7d) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/서비스경계.md` 생성 |
| 아키텍처 | [[아키텍처]](https://app.notion.com/p/3cd73873401a801ca91dd9be3211bc85) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/아키텍처.md` 생성 |
| ERD | [[ERD]](https://app.notion.com/p/ERD-3cd73873401a80059475e3bedc47e912) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/ERD.md` 생성 |
| API | [[API]](https://app.notion.com/p/API-3cd73873401a8050a341db921cf70443) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/API.md` 생성 |
| 권한 Matrix | [[권한 Matrix]](https://app.notion.com/p/3cd73873401a8058adfec5629d484b70) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/권한매트릭스.md` 생성 |
| 시퀀스 | [[시퀀스]](https://app.notion.com/p/3cd73873401a809e80b1f87ac8301e54) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/시퀀스.md` 생성 |
| 테스트 전략 | [[테스트 전략]](https://app.notion.com/p/3cd73873401a80b29a18e302583fbd45) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/테스트전략.md` 생성 |
| 테스트 체크리스트 | [[테스트 체크리스트]](https://app.notion.com/p/3cd73873401a80df8824cc9efca3641b) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/테스트체크리스트.md` 생성 |
| 실행·배포 가이드 | [[실행·배포 가이드]](https://app.notion.com/p/3cd73873401a80a4be9fc747d7acd744) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/배포가이드.md` 생성 |
| 트러블슈팅 | [[트러블슈팅]](https://app.notion.com/p/3cd73873401a802781f3c9e0687203b4) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/트러블슈팅.md` 생성 |
| Sprint Review | [[Sprint Review](https://app.notion.com/p/3cd73873401a80e5b086fa304edf53c9)] | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/스프린트리뷰.md` 생성 |
| Sprint Retrospective | [[Sprint Retrospective]](https://app.notion.com/p/retrospective-3cd73873401a809c8c66c26e9ea7f761) | 곽승욱, 변재웅, 정선우, 박준성 | 없음 — Sprint Review 뒤 `docs/retrospective.md` 생성 |

## 첫날 확인

- [ ] 15종 Notion 페이지가 있고 프로젝트 검토자가 열람할 수 있습니다.
- [ ] 각 페이지 URL과 담당자가 위 표에 등록되어 있습니다.
- [ ] 팀 Repository의 `README.md`가 이 인덱스와 GitHub Project·Issue를 연결합니다.
- [ ] Git `docs/`에는 아직 15개 본문을 만들지 않았습니다.

## Sprint Review 뒤 동기화

1. Review 결과를 먼저 Notion 원본에 반영합니다.
2. 확정된 15종 현재 내용을 같은 `docs/*.md` 경로에 복사합니다.
3. 각 Snapshot 상단에 Notion 원본 URL·Snapshot 기준 시점·동기화 시각·직접 편집 금지를 기록합니다.
4. 위 표의 최근 Git Snapshot을 실제 상대 링크로 바꾸고 문서 PR을 만듭니다.
5. 수정이 필요하면 Git 파일을 직접 고치지 않고 `Notion 수정 → Git 재동기화` 순서를 지킵니다.

별도 Sprint 폴더를 만들지 않습니다. Sprint별 내용은 Git commit 이력으로 보존하고 Week 4에 최종 Notion 내용을 다시 동기화합니다. Secret·Token·Cookie·개인정보는 Notion과 Git 어디에도 기록하지 않습니다.
