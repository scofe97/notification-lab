# notification-lab 로드맵 — 진행단계의 SSOT

이 문서는 프로젝트 전체를 단계(Phase)로 나누고 **지금 어디에 있는지**를 한 곳에서 보여줍니다. UC별 상세 현황은 [a-notification/docs/uc/00-index.md](a-notification/docs/uc/00-index.md), 빌드·검증 기록은 [a-notification/PROGRESS.md](a-notification/PROGRESS.md), 아키텍처는 [a-notification/docs/03-architecture.md](a-notification/docs/03-architecture.md)에 있습니다.

## 전체 단계

| Phase | 이름 | 상태 | 핵심 산출물 |
|-------|------|------|------------|
| **1** | 축 A 발송 파이프라인 (UC-1) | ✅ 완료 (2026-07-09) | 발송 파이프라인 코드 + 수동 E2E 검증 기록 |
| **2** | 축 A 마무리 (UC-2~5 + 테스트 자동화) | 🔜 진행 예정 | E2E 자동 테스트, 이력·아카이빙 구현 |
| **3** | LGMT Observability 스터디 | 🔜 다음 국면 (1개월) | 대시보드·알림·Runbook·실험 기록 |
| **4** | 축 B 사내 CMP 플랫폼 | ⏸ 보류 | 착수 시 확정 |

> 순서 원칙: **구현을 먼저 끝내고 모니터링 스택을 도입합니다.** Phase 2가 끝나야 Phase 3의 관측 대상이 완성됩니다. 단, Phase 2의 전부가 Phase 3의 선행 조건은 아닙니다 — 발송 파이프라인(UC-1)만으로도 LGMT 실험 대부분이 가능하므로, Phase 2가 길어지면 UC-3·5는 Phase 3와 병행할 수 있습니다.

---

## Phase 1 — 축 A 발송 파이프라인 ✅

원본(사내 알림 서비스)의 발송 흐름을 오픈소스로 재현했습니다.

- **구현**: Kafka `notification` 소비(`@KafkaListener`) → 채널 설정 캐시(Caffeine) → 채널 그룹핑 발송(`@CircuitBreaker` + OpenFeign) → WireMock 목 API. 실패 시 재시도 2회 후 `notification.DLT` 격리(`DeadLetterPublishingRecoverer`). 웹서버는 Undertow.
- **검증**: 수동 E2E 통과 (2026-07-09) — 수신자 2명(SMS·EMAIL) 이벤트 발행 → WireMock 양 채널 각 1건 도달. 기록: [PROGRESS.md](a-notification/PROGRESS.md) §UC-1 런타임 검증 결과.
- **문서**: 요구([01](a-notification/docs/01-requirements.md))·유스케이스([02](a-notification/docs/02-actors-usecases.md))·아키텍처([03](a-notification/docs/03-architecture.md)) + UC별 리뷰 노트([docs/uc/](a-notification/docs/uc/00-index.md)).

## Phase 2 — 축 A 마무리 🔜

UC-1 외의 유스케이스와 테스트 자동화를 끝내 관측 대상 서비스를 완성합니다. 권장 순서와 착수 조건입니다.

| 순서 | 작업 | 착수 조건 | 산출물 |
|------|------|----------|--------|
| 2-1 | **E2E 테스트 자동화** (Testcontainers) | 없음 — 바로 가능. 의존성은 이미 build.gradle에 있음 | 발행→발송 검증·실패→DLT·캐시 히트 자동 테스트. 수동 검증의 박제 |
| 2-2 | **UC-4 알림채널 설정 CRUD** | 없음 — Entity(`ChannelSetting`)는 존재, REST 계층만 추가 | 설정 조회·저장 API + 캐시 무효화 확인 |
| 2-3 | **UC-2 외부 REST 발송** | UC-4와 독립 | REST 진입점 → 기존 발송 서비스 재사용 |
| 2-4 | **UC-3 이력 조회 + UC-5 아카이빙** | OpenSearch 컨테이너(compose에 있음) 연결 | `history` 패키지 — OpenSearch 색인·조회·`@Scheduled` export |

> UC별 코드 리뷰 렌즈·실측법은 각 [docs/uc/UC-N.md](a-notification/docs/uc/00-index.md)에 이미 준비돼 있습니다. 구현하면서 ④ 확인 기록을 채웁니다.

## Phase 3 — LGMT Observability 스터디 🔜 (1개월)

a-notification을 관측 대상으로 삼아 Loki·Grafana·Mimir·Tempo를 도입하고, 장애를 의도적으로 만들어 원인을 증거로 좁혀가는 실험을 합니다. 별도 저장소를 만들지 않고 **이 저장소 안에서 진화**합니다.

핵심 메시지: **"모니터링은 문제가 있음을 알려주는 일이고, 옵저버빌리티는 왜 문제가 생겼는지 증거로 좁혀가는 일이다."**

### 주차별 골격

| 주차 | 목표 | 산출물 |
|------|------|--------|
| 1주 | **관측 설계** — 관측 목표·Signal Catalog·장애 시나리오 5종·Loki label 정책 정의 | `docs/observability/` 설계 문서 세트 |
| 2주 | **관측 연결** — LGMT compose 추가, Actuator·Micrometer·OTel 연결, Scenario Runner 기본 구현 | LGMT 기동 + 정상 처리 대시보드 + traceId 로그-트레이스 연결 확인 |
| 3주 | **장애 실험** — Poison Message·외부 API 5xx/delay·Consumer Lag·Cache Hit/Miss·JVM 병목 | `experiments/` 실험별 재현법 + metric·log·trace 증거 + 원인 판단 |
| 4주 | **운영 결과물화** — 대시보드 정리·Alert rule·Runbook·회고 | `dashboards/`·`alerts/`·`runbooks/`·final-review |

### 현재 코드와 스터디 목표의 간극

이 표가 2주차 작업 목록의 출발점입니다.

| 항목 | 현재 (Phase 1 완료 시점) | 스터디 목표 |
|------|--------------------------|------------|
| 관측 스택 | compose 스켈레톤만 존재(`infra/docker-compose.lgmt.yml`, 설정 파일 비어 있어 기동 불가). 앱 계측(Actuator·Micrometer·OTel) 미도입 | LGMT + Alloy 기동 + 앱 계측 연결 |
| DB | H2 파일모드 | MariaDB (`user_notification_channel_config` 등) — cache miss→DB latency→HikariCP 신호를 보려면 실제 DB 필요 |
| 부하 발생 | 수동 Kafka 발행 | Scenario Runner 서버 (정상·burst·poison·cache hit/miss 발행 + WireMock 모드 전환) |
| 토픽 | `notification` · `notification.DLT` | + `notification.retry` |
| 산출물 | 코드·UC 노트 | + dashboards·alerts·runbooks·experiments |

### 우선순위

- **반드시**: consumer lag 관측 · retry/DLT 관측 · WireMock 5xx/timeout/delay 재현 · CircuitBreaker 상태 관측 · Caffeine hit/miss 관측 · traceId 기반 log-trace 연결 · Grafana 대시보드 · 장애 실험 문서 · Runbook
- **가능하면**: JVM 대시보드 · Loki label cardinality 실험 · DLT reprocessor · idempotency · kind 배포 · Pyroscope · OpenSearch↔Loki 비교

### 장애 원인 추적 지도 (스터디의 뼈대)

증상(DLT 증가 또는 Consumer Lag 증가)에서 출발해 이 순서로 분기합니다.

1. Feign error·timeout·latency 증가? → 외부 API 장애 가능성. WireMock 모드·Feign span·CB 상태 확인
2. 아니면 Validation/Deserialization error 증가? → Poison Message 가능성. Loki에서 failureReason·topic/partition/offset 확인
3. 아니면 Cache miss·DB latency 증가? → DB/cache 병목 가능성. hit ratio·query latency·HikariCP active/pending 확인
4. 아니면 JVM CPU·GC pause·thread 이상? → 애플리케이션 내부 병목 가능성. heap·GC·Tempo internal span 확인
5. 전부 정상? → consumer 설정 확인 (concurrency·partition 수·skew·retry/backoff)

## Phase 4 — 축 B 사내 CMP 플랫폼 ⏸

자원 조회·관리 플랫폼(사내 CMP 플랫폼) 재현. 착수 시 범위를 확정합니다. Phase 3와의 선후 관계는 그때 판단합니다.
