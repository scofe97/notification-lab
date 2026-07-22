# LGMT Observability 스터디 계획

3단계의 목적은 Kafka 알림 파이프라인에서 지연·실패·DLT·CircuitBreaker·캐시·DB 문제가 발생했을 때, 어떤 증거로 원인을 좁히는지 학습하는 것입니다. 서비스 기능을 넓히는 대신 관측 신호와 운영 판단을 결과물로 남깁니다.

## 스터디 역할

| 대상 | 역할 | 상태 |
|------|------|------|
| `apps/notification-service` | Kafka 소비, 캐시·DB 조회, 외부 발송, CircuitBreaker 적용 | ✅ 기존 |
| `apps/notification-scenario-runner` | 정상·burst·poison 발행, WireMock 장애 모드 전환, scenario history 기록 | 🟡 스캐폴딩 완료 |
| Kafka | `notification` 소비와 DLT 격리 | ✅ 기존 |
| MariaDB | cache miss가 DB·HikariCP 신호로 이어지는지 관측 | 🔜 observability profile |
| WireMock | 외부 API 200·5xx·timeout·delay 모사 | ✅ 기존 |
| LGMT | 로그·메트릭·트레이스 저장과 Grafana 탐색 | 🔜 observability profile |

## 주차별 산출물

> 주차는 난이도·의존 순서의 가이드이지 기한이 아닙니다. 관측 UC 번호 순서(쉽고 우선순위 높은 것부터)로 할 수 있는 만큼 진행합니다.

| 주차 | 목표 | 산출물 |
|------|------|--------|
| 1주 | 관측 설계와 정상 기준선 | Signal Catalog, failure scenario, Loki label 정책, 기준선 기록 |
| 2주 | 계측과 LGMT 연결 | Actuator·Micrometer·OTel, Alloy, 정상 dashboard, traceId log-trace 연결 |
| 3주 | 장애 주입과 원인 판단 | 관측 UC(01~12)의 재현법, metric·log·trace 증거, 실험 기록 |
| 4주 | 운영 결과물화 | dashboard JSON, alert rule, Runbook, final review |

## 결과물 구조

```text
notification-lab/
  apps/
    notification-service/              # ✅ 관측 대상
    notification-scenario-runner/      # 🟡 신호·장애 발생기 스캐폴딩
  infra/
    compose.yaml                       # 컴포넌트 compose 집계
    compose/                           # Kafka·OpenSearch·WireMock 등 분리 파일
  dashboards/                          # 🔜 Grafana dashboard JSON
  alerts/                              # 🔜 alert rule
  scenarios/                           # 🔜 반복 가능한 시나리오 정의
  experiments/                         # 🔜 재현법·증거·원인 판단
  runbooks/                            # 🔜 장애 조사 절차
```

## 우선순위

- **반드시**: consumer lag, retry/DLT, WireMock 5xx·timeout·delay, CircuitBreaker 상태, Caffeine hit/miss, consumer rebalance, traceId 기반 log-trace 연결, Grafana 대시보드, 장애 실험 문서, Runbook
- **후반 (JVM 축)**: GC 압박, 스레드 블로킹, OOM·heap dump — 관측 UC-09~11로 정규 편입(2026-07-21). 서비스 신호(외부 API·DB·consumer)로 설명되지 않는 병목을 다루므로 기본 관측이 자리 잡은 뒤 진행
- **가능하면**: DLT Reprocessor, eventId 기반 idempotency, dashboard·alert as code, kind/k3d 배포, Loki label cardinality 심화 실험
