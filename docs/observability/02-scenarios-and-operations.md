# 관측 시나리오와 운영 절차

장애 실험의 목적은 그래프를 늘리는 것이 아니라, 증상을 증거로 분해해 원인을 판단하는 것입니다. 각 실험은 재현 조건, metric·log·trace 증거, 원인 판단을 함께 기록합니다.

## 장애 시나리오

| 시나리오 | 재현법 | 기대 증거 | 실험 목적 |
|----------|--------|-----------|-----------|
| Poison Message | 잘못된 JSON, 필드 누락, 미지원 type 발행 | DLT·Validation/Deserialization error 증가, Feign·CB 변화 없음, `failureReason`과 topic/partition/offset | DLT 증가가 외부 API 장애만 의미하지 않음을 확인 |
| 외부 API 장애 | WireMock 500·503·timeout·delay 전환 | Feign error·latency, retry, CB failure rate·OPEN·not permitted, DLT, 실패 Feign span | 외부 장애가 retry·CB·DLT로 번지는 흐름 확인 |
| Consumer Lag | burst 발행 | produce rate > consume rate, lag·처리 latency 증가 | lag 자체가 아니라 병목 원인을 분기 |
| Cache Hit/Miss | 동일 userId 반복 또는 매번 다른 userId 발행 | hit 증가 시 DB query·latency 감소, miss 증가 시 HikariCP active/pending·lag 증가 가능 | 캐시를 운영 신호로 해석 |
| DB latency | cache miss 상태에서 조회 지연 또는 낮은 HikariCP pool 설정 | DB query latency·pending 증가, 외부 API 정상, 처리 latency·lag 증가 | Kafka lag가 DB 병목에서도 발생함을 확인 |

## 장애 원인 추적 지도

DLT 또는 Consumer Lag가 증가하면 아래 순서로 조사합니다.

1. Feign error·timeout·latency가 증가했는지 확인합니다. 증가했다면 WireMock 모드, Feign span, CircuitBreaker 상태를 확인합니다.
2. Validation·Deserialization error가 증가했는지 확인합니다. 증가했다면 Loki에서 `failureReason`, topic, partition, offset을 확인합니다.
3. Cache miss·DB latency가 증가했는지 확인합니다. 증가했다면 hit ratio, query latency, HikariCP active·pending을 확인합니다.
4. 앞의 신호가 정상이라면 consumer concurrency, partition 수, skew, retry/backoff를 확인합니다.

## Phase 3 관측 실험 UC

| UC | 실험 | 핵심 증거 | 목표 기록 |
|----|------|-----------|-----------|
| UC-01 | 정상 처리 기준선 | 처리량·p95 latency·lag·cache hit ratio | `01-normal-baseline.md` |
| UC-02 | 외부 API 지연 | Feign latency·consumer lag·Tempo span | `02-external-delay.md` |
| UC-03 | 외부 API 5xx·CircuitBreaker·DLT | 5xx·CB state·not permitted·DLT | `03-external-5xx-circuitbreaker.md` |
| UC-04 | Poison Message | validation/deserialization error·DLT header | `04-poison-message-dlt.md` |
| UC-05 | 대량 발행·consumer concurrency | produce/consume rate·lag·partition·concurrency | `05-consumer-lag.md` |
| UC-06 | Cache Hit/Miss | hit ratio·DB query·처리 시간 | `06-cache-hit-miss.md` |
| UC-07 | DB latency·HikariCP | query latency·active/pending·lag | `07-db-latency.md` |
| UC-08 | Loki label cardinality | stream 수·ingestion·query latency | `08-loki-label-cardinality.md` |

## 운영 결과물

- `dashboards/`: pipeline overview, external API·CircuitBreaker, cache·DB 대시보드
- `alerts/`: Consumer Lag, DLT 증가, 외부 API latency, CircuitBreaker OPEN 알림
- `runbooks/`: metric → log → trace 순서의 조사 절차
- `experiments/`: 재현 시각, scenario history, 증거 스크린샷과 원인 판단

JVM heap·GC·thread·CPU 분석은 기본 관측 결과가 쌓인 뒤, 외부 API·DB·consumer 설정으로 설명되지 않는 병목을 확인하는 후속 확장 후보로 둡니다.
