# notification-lab 로드맵 — 진행단계의 SSOT

이 문서는 프로젝트의 현재 단계와 다음 단계를 관리합니다. Phase 3의 상세 설계는 [관측 스터디 계획](docs/observability/00-study-plan.md), [아키텍처](docs/observability/01-architecture.md), [시나리오와 운영 절차](docs/observability/02-scenarios-and-operations.md)로 나눴습니다.

## 전체 단계

| Phase | 이름 | 상태 | 핵심 산출물 |
|-------|------|------|------------|
| **1** | 알림 발송 파이프라인 (UC-1) | ✅ 완료 (2026-07-09) | 발송 파이프라인 코드 + 수동 E2E 검증 기록 |
| **2** | 알림 서비스 마무리 (UC-2~5 + 테스트 자동화) | 🔜 진행 예정 | E2E 자동 테스트, 이력·아카이빙 구현 |
| **3** | LGMT Observability 스터디 | 🔜 다음 국면 (1개월) | 대시보드·알림·Runbook·실험 기록 |

> 순서 원칙: 구현된 발송 파이프라인을 관측 대상으로 삼고, 문제가 있음을 보여주는 데서 멈추지 않고 왜 발생했는지를 증거로 좁혀갑니다. UC-1만으로도 Phase 3의 핵심 실험을 시작할 수 있습니다.

## Phase 1 — 알림 발송 파이프라인 ✅

Kafka `notification` 소비(`@KafkaListener`) → 채널 설정 캐시(Caffeine) → 채널별 발송(`@CircuitBreaker` + OpenFeign) → WireMock 목 API 흐름을 구현했습니다. 처리 실패는 1초 간격으로 2회 재시도한 뒤 `notification.DLT`로 격리합니다.

- **검증**: 수동 E2E 통과 (2026-07-09) — 수신자 2명(SMS·EMAIL) 이벤트 발행 뒤 WireMock 양 채널에 각각 1건이 도달했습니다. 기록은 [PROGRESS.md](apps/notification-service/PROGRESS.md)에 있습니다.
- **문서**: [요구사항](apps/notification-service/docs/01-requirements.md), [유스케이스](apps/notification-service/docs/02-actors-usecases.md), [아키텍처](apps/notification-service/docs/03-architecture.md), [UC별 리뷰 노트](apps/notification-service/docs/uc/00-index.md)를 유지합니다.

## Phase 2 — 알림 서비스 마무리 🔜

| 순서 | 작업 | 산출물 |
|------|------|--------|
| 2-1 | E2E 테스트 자동화 (Testcontainers) | 발행→발송, 실패→DLT, 캐시 히트 검증 |
| 2-2 | ✅ UC-4 알림채널 설정 CRUD (2026-07-20) | GET/PUT API + `@CachePut` 즉시 반영 검증. 2026-07-21 헥사고날 `channel` 컨텍스트로 분리 |
| 2-3 | ✅ UC-2 외부 REST 발송 (2026-07-21) | `dispatch` 컨텍스트 — 수신자 조회(Feign) → 기존 발송 재사용, 응답 코드 집계 |
| 2-4 | UC-3 이력 조회 + UC-5 아카이빙 | OpenSearch 색인·조회·스케줄 기반 export |

## Phase 3 — LGMT Observability 스터디 🔜

`notification-service`는 관측 대상이고, `notification-scenario-runner`는 신호와 장애를 만드는 실험 발생기입니다. Phase 3은 알림 기능을 더 만드는 단계가 아니라 지연·실패·DLT·캐시·DB 문제를 metric → log → trace 순서로 해석하는 스터디입니다.

- [관측 스터디 계획](docs/observability/00-study-plan.md): 목표, 주차별 산출물, 우선순위
- [관측 아키텍처](docs/observability/01-architecture.md): 구성요소와 발전 아키텍처
- [시나리오와 운영 절차](docs/observability/02-scenarios-and-operations.md): 실험 UC, 증거, 원인 추적 지도
