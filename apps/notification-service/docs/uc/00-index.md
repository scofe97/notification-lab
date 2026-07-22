# UC별 리뷰·연구 노트 — 인덱스

이 폴더는 UC마다 한 문서를 두고 **구현·리뷰·연구를 손으로 확인하는 절차**를 기록합니다. UC 명세(주 흐름·시퀀스)는 상위 [../02-actors-usecases.md](../02-actors-usecases.md), 설계는 [../03-architecture.md](../03-architecture.md), 요구사항은 [../01-requirements.md](../01-requirements.md)에 있습니다.

각 UC 문서 구성: **① 개념 역링크 → ② 코드 리뷰 렌즈 → ③ 직접 관찰·실측법 → ④ 확인 기록(직접 채움)**.

> **UC 번호는 명세 ID이지 학습 순서가 아니다.** 번호(UC-1~5)는 안 바뀌는 식별자이고, "무엇을 언제 배우는가"의 진행 순서는 [ROADMAP](../../../../ROADMAP.md)의 단계가 유일 SSOT다. 실제 진행은 UC-1 → UC-4 → UC-2 순이었다. 각 UC 문서 상단의 "학습 순서" 줄이 선행·후속 의존을 가리킨다.

## 문서 목록 · 구현 현황판

구현 범위 열의 클래스는 `src/main/java/com/practice/notification/` 아래 컨텍스트별 패키지(send·channel·dispatch·history·common)에 있습니다. Phase 번호는 [../../../../ROADMAP.md](../../../../ROADMAP.md)의 단계입니다.

| 문서 | UC | 상태 | 핵심 기술 | 구현 범위 (소스) | 검증 | 다음 작업 |
|------|-----|------|-----------|------------------|------|-----------|
| [UC-1.md](UC-1.md) | Kafka 알림 발송 | **구현+검증 완료** | @KafkaListener·Caffeine·OpenFeign·Resilience4j | `send` 전 패키지 — listener/`NotificationListener` · service/`NotificationSendService` · remote/`NotificationSendClient`·`NotificationSendCaller` · config/`KafkaConsumerConfig`·`CacheConfig` · domain 3종 — 및 `channel` 컨텍스트의 `ChannelSettingService`(2026-07-21 헥사고날 분리) | 수동 E2E (2026-07-09) + 실측 실습·회로차단 수정 검증 (2026-07-20) | E2E 자동화 — Testcontainers (2단계-1) |
| [UC-1-dlt.md](UC-1-dlt.md) | └ 실패 경로(DLT 관찰) | **적재·재처리 실측 완료** | DeadLetterPublishingRecoverer | config/`KafkaConsumerConfig` (재시도 2회 → `notification.DLT`) | 독약·5xx 적재, 헤더 원인, 재처리 성공/실패 조건 확인 (2026-07-20) | 자동 재처리 도구는 3단계 후보 |
| [UC-2.md](UC-2.md) | 외부 시스템 REST 발송 | **구현+스모크 검증** (2026-07-21) | OpenFeign(수신자 조회)·헥사고날 | `dispatch` 컨텍스트(api·application·domain+port·infrastructure) — send는 완충 어댑터로 재사용 | 스모크 — 200 집계·400·404, 조회→발송 카운트 연쇄 | 이해 루프 + 207/502 실측 |
| [UC-3.md](UC-3.md) | 알림 이력 조회 | **구현+스모크 검증** (2026-07-22) | PostgreSQL(JPA)·ULID | `history` 컨텍스트 api·application·domain+port·infrastructure/persistence | 스모크 — 두 입구(REST·Kafka) 발송분 조회, 최신순 | 이해 루프 + OpenSearch 어댑터는 후속 후보 |
| [UC-4.md](UC-4.md) | 알림채널 설정 | **구현+검증 완료** (2026-07-21) | JPA 복합키·REST CRUD·`@CachePut` | `channel` 컨텍스트(api·application·domain·infrastructure — 2026-07-21 헥사고날 분리) | 스모크 E2E — GET 기본값·PUT 즉시 반영 (캐시 갱신) | 400 응답·키 불일치 실측 + ArchUnit 가드 |
| [UC-5.md](UC-5.md) | 로그 아카이빙 | **구현+스모크 검증** (2026-07-22) | @Scheduled·NDJSON export | `history` 컨텍스트 — 실시간 기록(FR-8) + cron·수동 재실행 export(FR-11) | 스모크 — 4건 export, NDJSON 파일 확인 | 이해 루프 + 저장소 삭제 정책 결정 |
| [undertow.md](undertow.md) | (전역 기반) 내장 웹서버 | 적용 완료·실측 대기 | Undertow ↔ Tomcat | `build.gradle` (Tomcat 제외 + Undertow starter) | 기동 로그 `Undertow started on port 8092` 확인 | Tomcat 대비 실측 |

> `undertow.md`는 특정 UC가 아니라 프로젝트 전역 기반이라 UC 번호 없이 둡니다. 지금 집중 연구 대상.
> 진행 상태·빌드는 [../../PROGRESS.md](../../PROGRESS.md), 메시지 처리 매핑 등 프로젝트 전역 개념은 [../../NOTES.md](../../NOTES.md), 단계(Phase) 로드맵은 [../../../../ROADMAP.md](../../../../ROADMAP.md)에 있습니다.

## 3단계 관측 실험 UC

아래 UC는 서비스 기능 명세가 아니라 **재현 조건 → 관측 증거 → 원인 판단 → 실험 기록**을 반복하는 학습 단위입니다. 상세 설계는 [관측 시나리오와 운영 절차](../../../../observability/docs/02-scenarios-and-operations.md)에 있습니다. 개별 실험 기록은 3단계에서 `experiments/`에 추가합니다.

| UC | 실험 | 핵심 증거 | 목표 산출물 |
|----|------|-----------|-------------|
| UC-01 | 정상 처리 기준선 + JVM 패널 | 처리량·p95 latency·lag·cache hit ratio·JVM 기준값 | `01-normal-baseline.md` |
| UC-02 | Poison Message | validation/deserialization error·DLT header | `02-poison-message-dlt.md` |
| UC-03 | 외부 API 5xx·CircuitBreaker·DLT | 5xx·CB state·not permitted·DLT | `03-external-5xx-circuitbreaker.md` |
| UC-04 | 외부 API 지연 | Feign latency·consumer lag·Tempo span | `04-external-delay.md` |
| UC-05 | Cache Hit/Miss | hit ratio·DB query·처리 시간 | `05-cache-hit-miss.md` |
| UC-06 | 대량 발행·consumer concurrency | produce/consume rate·lag·partition·concurrency | `06-consumer-lag.md` |
| UC-07 | DB latency·HikariCP | query latency·active/pending·lag | `07-db-latency.md` |
| UC-08 | Consumer rebalance | rebalance 로그·lag spike·재전달 중복 | `08-consumer-rebalance.md` |
| UC-09 | GC 압박 → lag 전파 | jvm_gc_pause·처리 latency·lag | `09-gc-pressure.md` |
| UC-10 | 스레드 블로킹 관찰 | jvm_threads states·thread dump·처리량 정체 | `10-thread-blocking.md` |
| UC-11 | OOM·heap dump 분석 | OOM 로그·재시작·heap dump 지배 객체 | `11-oom-heap-dump.md` |
| UC-12 | Loki label cardinality | stream 수·ingestion·query latency | `12-loki-label-cardinality.md` |

번호는 쉽고 우선순위 높은 것부터의 진행 순서입니다(2026-07-21 재정렬, UC-09~11은 JVM 축). 상세는 [관측 시나리오](../../../../observability/docs/02-scenarios-and-operations.md)가 SSOT입니다.
