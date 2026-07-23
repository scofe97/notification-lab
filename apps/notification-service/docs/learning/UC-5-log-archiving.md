# UC-5 · 로그 아카이빙 — 실시간 기록, @Scheduled 배치, NDJSON export

> 발송 이력을 만들고(FR-8 기록) 보관하는(FR-11 export) 흐름을 학습 단위로 다룹니다. 사람 요청이 아니라 시간이 트리거라는 점, 그리고 기록 실패·배치 재실행을 어떻게 다루는지가 핵심입니다. 이 기록을 읽는 쪽은 [UC-3 학습 문서](UC-3-history-query.md)가 다룹니다.

- 상태: **구현 + 스모크 검증 완료 (2026-07-22)**. 구현 전 계획(OpenSearch)과 달리 저장은 RDB(PostgreSQL)로 선 구현. 색인(FR-8)은 배치가 아니라 **발송 직후 실시간 기록**으로 구현했고, export는 매일 03:00 cron + 수동 재실행 API 이중 경로입니다.
- 근거: 구현 커밋 `f662466`·`826e228`, 2026-07-22 스모크, [UC-5 리뷰 노트](../uc/UC-5.md)
- 연결: [UC-1 학습 문서](UC-1-kafka-notification.md)(이력을 만드는 발송 흐름) · [UC-3 학습 문서](UC-3-history-query.md)(이 기록을 조회하는 쪽) · [컨벤션](../../../../AGENTS.md)
- 학습 순서: 선행 → UC-1(발송 이력 생산). 후속 → UC-3(조회). 진행 순서 SSOT는 [ROADMAP](../../../../ROADMAP.md).



## 구현 개요 — 먼저 읽는 지도

구현 범위는 셋입니다. ① 발송 직후 목적지별 이력 기록(FR-8·9 — send가 history in-port 호출, **best-effort 계약**), ② `@Scheduled` cron(기본 03:00)이 전일 이력을 NDJSON 파일로 export(FR-11), ③ 놓친 날짜를 보완하는 수동 재실행 API.

| 구성요소 | 파일 | 역할 |
|---|---|---|
| 기록 in-port | `history/domain/port/in/RecordNotificationHistoryUseCase.java` | 발송 직후 기록 진입 계약 — **실패를 호출자에게 전파하지 않음** |
| 기록 호출자 | `send/application/NotificationSendService.java` ④단계 | 채널 발송 결과를 목적지별 기록으로 위임 |
| 기록·조회 구현 | `history/application/NotificationHistoryService.java` | ULID 부여, 저장 실패는 경고 로그로 흡수 |
| 스케줄 트리거 | `history/api/HistoryArchiveScheduler.java` | cron으로 "전일" 결정 후 in-port 호출 — inbound adapter(액터=시간) |
| 수동 재실행 | `history/api/NotificationHistoryController.java` | `POST /api/history/archive/{day}` — 놓친 날짜 보완 |
| 아카이브 구현 | `history/application/NotificationHistoryArchiveService.java` | 전일 조회 → 파일 쓰기 위임, 건수 반환 |
| out-port 2개 | `history/domain/port/out/NotificationHistoryStorePort·ArchiveWriterPort.java` | 저장소 계약 / 보관 파일 계약 |
| 파일 어댑터 | `history/infrastructure/archive/NdjsonArchiveWriter.java` | `notification-history-{날짜}.ndjson`, 같은 날짜 재실행은 덮어쓰기 |

검증 상태: 두 입구 발송분의 기록과 수동 트리거 export(4건, 파일 내용)는 스모크로 확인했습니다. cron 자동 발화·기록 실패 흡수·덮어쓰기 멱등은 실행으로 보지 않았습니다(아래 표).



## 증거 등급

**확인됨**: 실행해서 관찰했다 · **코드상 추론**: 코드·설정상 필연이지만 실행으로 보진 않았다 · **미검증**: 코드를 읽어도 확정할 수 없다.

| 주장 | 등급 | 근거 |
|---|---|---|
| 발송 직후 두 입구(REST·Kafka) 모두 목적지별 이력이 기록된다 | 확인됨 | 2026-07-22 스모크 — rest-UUID 3건 + evt-history-1 1건 |
| 수동 트리거로 해당 날짜 이력이 NDJSON 파일로 export된다 | 확인됨 | 스모크 — `archived: 4`, 파일 4줄 내용 확인 |
| cron(03:00)이 전일 아카이브를 자동 실행한다 | 코드상 추론 | `@Scheduled` + `@EnableScheduling` 선언 — 실제 발화는 관찰 안 함 |
| 이력 저장 실패는 발송에 전파되지 않는다 (best-effort) | **확인됨 (2026-07-22 실측)** | PostgreSQL 정지 중 발송 → WireMock 도달 +1·재시도 0·WARN 삼킴·정상 커밋, 해당 이력(evt-be-1)만 유실 실증 |
| 같은 날짜 재실행은 파일 덮어쓰기라 안전하다 (멱등) | **확인됨 (2026-07-22 실측)** | 연속 2회 트리거 — `archived: 6` / 파일 6줄 동일 |
| 삼켜지는 실패도 시간은 잡아먹는다 — 기록 실패 판정까지 리스너 스레드가 블록된다 | **확인됨 (2026-07-22 실측, 보너스 발견)** | 수신 17:26:30 → WARN 17:27:00 — HikariCP `connectionTimeout` 기본 30초 대기 동안 Kafka 리스너 스레드 점유(후속 메시지 지연) |
| 저장소의 이력은 export 후에도 남는다 (삭제 보류) | 확인됨 | export 후 UC-3 조회가 같은 건을 반환 |



## 후속 검증

| 항목 | 상태 | 확인 방법 |
|---|---|---|
| cron 자동 발화 | 대기 | cron을 짧은 주기로 임시 변경해 트리거·파일 생성 관찰 (후 원복) |
| best-effort 실측 | ✅ 확인됨 (2026-07-22) | DB 정지 실험 — 위 증거 등급 표 |
| 재실행 멱등 | ✅ 확인됨 (2026-07-22) | 연속 2회 트리거 — 위 증거 등급 표 |
| 저장소 삭제 정책 | 대기 | export 후 원본 삭제 여부·보존 기간 결정 (현재 보류) |
| 기록 블록 지연 개선 | 대기 | Hikari `connectionTimeout` 축소 또는 기록 비동기화(@Async/이벤트) — 30초 리스너 블록이 consumer lag으로 번지는 것 완화 |
| 배치 미실행 감지 | 대기 | "전일 파일 존재 확인" 알림 등 — 수동 재실행 API를 누를 조건인 '감지'가 현재 없음 (Phase 3 관측 소재) |



## Phase 진행 기록

> 각 Phase는 대화로 진행하고, 여기에는 통과 여부와 해결된 오해만 남깁니다. 개인 답변 원문은 기록하지 않습니다.

- [x] Phase 1 · 맥락과 예측 — cron 미발화(자동 만회 없음)·수동 API·감지 빈틈 3종을 예측으로 도출 (2026-07-22)
- [x] Phase 2 · 안내된 흐름 읽기 — 포트 5종 전체 리뷰. "try-catch는 1:N의 1(구현)에 둔다"를 자력 도출 (2026-07-22)
- [x] Phase 3 · 실패·경계 추적 — 같은 DB 장애에 세 경로가 다르게 반응(기록=삼킴·조회=500·아카이브=로그 후 대기)함을 추적 (2026-07-22)
- [x] Phase 4 · 실측 실습 — best-effort·멱등 확인 + 30초 블록 보너스 발견 (2026-07-22)
- [x] Phase 5 · 능동 인출 — 정식 5문항 (2026-07-23, UC-3과 통합). 자력: 계약 위치·시간 액터 3종·어댑터 교체 지점

해결된 오해: ① 커넥션 풀의 30초는 "판정이 늦는 것"이 아니라 "포기를 늦게 하도록 설정된 것"(혼잡 대비 관용) ② best-effort의 뜻 — 최선은 다하되 보장하지 않음, 실패해도 흐름을 막지 않음 ③ ULID의 다중 인스턴스 안전성 — 타임스탬프 48비트+랜덤 80비트라 조율 없이 충돌 사실상 0.

재복습 권고(다음 세션 첫 5분): ① 포트의 in/out은 문패 주인(컨텍스트) 기준 — "남의 집에 들어가면 그 집의 현관(in)" ② 삼켜지는 실패도 시간은 잡아먹는다(30초 블록→lag) ③ 조회는 보호 없이 1회 시도→500.
