# UC-3 · 알림 이력 조회 — PostgreSQL 기간 검색, ULID 정렬

> 사용자가 채널·기간으로 발송 이력을 검색하는 흐름(FR-10)을 학습 단위로 다룹니다. 이력을 만드는 쪽(기록·아카이빙)은 [UC-5 학습 문서](UC-5-log-archiving.md)가 다루며, 두 UC는 같은 `history` 컨텍스트를 공유합니다.

- 상태: **구현 + 스모크 검증 완료 (2026-07-22)**. 구현 전 계획(OpenSearch·채널별 쿼리 매퍼)과 달리 **저장은 RDB(PostgreSQL)로 선 구현** — OpenSearch는 `NotificationHistoryStorePort` 어댑터 교체 후보로 보류했고, 채널별 쿼리 매퍼(`QueryParamMapperFactory`)는 RDB의 파라미터 쿼리로 충분해 생략했습니다(채널마다 쿼리 형식이 갈리는 건 검색엔진 DSL의 사정이었음).
- 근거: 구현 커밋 `f662466`·`826e228`, 2026-07-22 스모크, [UC-3 리뷰 노트](../uc/UC-3.md)
- 연결: [UC-5 학습 문서](UC-5-log-archiving.md)(이력을 기록·보관하는 쪽) · [UC-1 학습 문서](UC-1-kafka-notification.md)(이력을 만드는 발송 흐름) · [컨벤션](../../../../AGENTS.md)
- 학습 순서: 선행 → UC-5(기록 구조). 진행 순서 SSOT는 [ROADMAP](../../../../ROADMAP.md).



## 구현 개요 — 먼저 읽는 지도

구현 범위는 둘입니다. ① `GET /api/history?channelType=&from=&to=` REST 계약(일 단위 기간, 양끝 포함, 최신순), ② `history` 컨텍스트의 조회 경로(in-port → application → out-port → PostgreSQL 어댑터).

| 구성요소 | 파일 | 역할 |
|---|---|---|
| REST 어댑터 | `history/api/NotificationHistoryController.java` | 채널·기간 조건 접수, in-port 호출 (아카이브 수동 재실행 API도 동거) |
| 응답 DTO | `history/api/HistoryResponse.java` | id(ULID)·eventId·채널·목적지·성공여부·발송시각 |
| in-port | `history/domain/port/in/SearchNotificationHistoryUseCase.java` | 조회 진입 계약 |
| 유스케이스 구현 | `history/application/NotificationHistoryService.java` | 조회는 out-port 위임 (기록 유스케이스와 동거) |
| 도메인 모델 | `history/domain/model/NotificationHistory.java` | 순수 POJO — ULID id(FR-9), 목적지 단위 1건 |
| out-port | `history/domain/port/out/NotificationHistoryStorePort.java` | 저장·검색·전일조회 계약 — OpenSearch 전환 시 이 뒤만 교체 |
| JPA 어댑터 | `history/infrastructure/persistence/NotificationHistoryEntity·JpaRepository·PersistenceAdapter.java` | `notification_history` 테이블, (channelType, sentAt) 인덱스, 일→시각 범위 변환 |

검증 상태: 채널·기간 조회가 REST·Kafka 두 입구의 발송분을 최신순으로 돌려주는 것까지 스모크로 확인했습니다. 파라미터 오류 응답·대량 결과 동작은 실행으로 보지 않았습니다(아래 표).



## 증거 등급

**확인됨**: 실행해서 관찰했다 · **코드상 추론**: 코드·설정상 필연이지만 실행으로 보진 않았다 · **미검증**: 코드를 읽어도 확정할 수 없다.

| 주장 | 등급 | 근거 |
|---|---|---|
| 채널·기간 조회가 두 입구(REST·Kafka) 발송분을 모두 돌려준다 | 확인됨 | 2026-07-22 스모크 — SMS 3건(rest-UUID 2 + evt-history-1) |
| 결과가 최신순으로 정렬된다 | 확인됨 | 스모크 — sentAt 내림차순 관찰 |
| 기간은 일 단위 양끝 포함이다 (to일 23:59:59.999…까지) | 코드상 추론 | 어댑터의 `endOfDay` 변환 — 경계값(자정 직전/직후) 실측 안 함 |
| 날짜 형식 오류·필수 파라미터 누락은 400으로 거부된다 | 코드상 추론 | 스프링 바인딩 기본 동작 — 실제 응답 확인 안 함 |
| 결과가 아주 많아도 전부 반환한다 (페이징 없음) | 코드상 추론 | 페이징 미구현 — 대량 조회 시 응답 크기·메모리 부담은 구조적 한계 |
| 저장소 장애 시 조회는 보호 장치 없이 1회 시도 → 500 | **확인됨 (2026-07-22 실측)** | PostgreSQL 정지 중 GET → http=500. 재시도·회로차단기·폴백 없음 — UC-2 수신자 조회와 같은 개선 후보 자리 |



## 후속 검증

| 항목 | 상태 | 확인 방법 |
|---|---|---|
| 채널별 쿼리 분기 (구 계획) | **계획 변경으로 소멸** | RDB 채택으로 매퍼 생략 — OpenSearch 도입 시 재검토 |
| 기록·조회 대상 일치 | ✅ 확인됨 (2026-07-22) | UC-5가 기록한 건을 UC-3 조회가 그대로 읽음 (스모크) |
| 기간 경계값 | 대기 | 자정 직전·직후 이력으로 from/to 경계 포함 여부 확인 |
| 페이징 부재 | 대기 | 대량 이력에서 응답 크기 관찰 — Pageable 도입 판단 재료 |



## Phase 진행 기록

> 각 Phase는 대화로 진행하고, 여기에는 통과 여부와 해결된 오해만 남깁니다. 개인 답변 원문은 기록하지 않습니다.

- [x] Phase 1 · 맥락과 예측 — UUID의 정렬 불가를 자력 도출, auto-increment의 "단일 마스터 무충돌" 반박을 통해 진짜 이유(저장소 이식성·생성 시점)를 정리 (2026-07-22)
- [x] Phase 2 · 안내된 흐름 읽기 — history 포트 5종 전체 리뷰에 포함 (2026-07-22, UC-5 문서와 통합)
- [x] Phase 3 · 실패·경계 추적 — 저장소 장애 시 조회 500(보호 부재)을 추적 (2026-07-22)
- [x] Phase 4 · 실측 실습 — DB 정지 중 조회 500 실측 (2026-07-22). 기간 경계·400은 대기 유지
- [x] Phase 5 · 능동 인출 — 정식 5문항 통과 (2026-07-23, UC-5와 통합 — 결산·해결된 오해·재복습은 [UC-5 문서](UC-5-log-archiving.md#phase-진행-기록) 참조)

이 문서 고유의 해결된 오해: ULID의 다중 인스턴스 안전(타임스탬프+랜덤 80비트, 조율 불요)과 "ID를 DB가 만들면 저장소를 못 갈아끼운다"는 이식성 원칙 — `sentAt`을 앱 시계로 찍는 것도 같은 원칙(DB 기능 비종속 + "발송 시각"이라는 의미 보존).
