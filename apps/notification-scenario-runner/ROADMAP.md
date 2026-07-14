# notification-scenario-runner 로드맵

`notification-scenario-runner`는 알림 서비스의 기능을 대체하지 않습니다. 정상 트래픽·부하·실패 조건을 재현해 관측 스터디에 **해석 가능한 입력과 실험 시점**을 제공하는 데이터 생성 모듈입니다.

전체 프로젝트 단계는 [루트 ROADMAP](../../ROADMAP.md), 관측 시나리오의 증거와 판단 절차는 [시나리오·운영 절차](../../docs/observability/02-scenarios-and-operations.md)에 있습니다.

## 역할과 경계

```text
Scenario Runner
  ├─ Kafka: 정상·burst·poison 메시지 발행
  ├─ WireMock: 200·5xx·timeout·delay 조건 전환
  ├─ Scenario history: 실험 시작·종료·파라미터를 구조화 로그로 기록
  └─ Control: 실험을 시작·중지하고 상태를 확인

notification-service
  └─ 메시지를 소비하고 실제 처리 신호(metric·log·trace)를 생성
```

Runner는 실패 원인을 판정하거나 운영 조치를 수행하지 않습니다. 그 역할은 Grafana 탐색과 Runbook에 남깁니다.

## 단계

| 단계 | 상태 | 목표 | 산출물·완료 기준 |
|---|---|---|---|
| 0. 모듈 골격 | ✅ 완료 | 독립 실행 가능한 Spring Boot 앱의 기반을 만듭니다. | Gradle wrapper, Java 21, Actuator health, Kafka 접속 설정, 기본 테스트 |
| 1. Kafka 메시지 발행 | 🔜 | 시나리오 타입에 따라 알림 이벤트를 생성하고 발행합니다. | `ScenarioType`, 이벤트 팩토리, Kafka producer, 정상·burst·poison 발행 테스트 |
| 2. WireMock 장애 제어 | 🔜 | 외부 의존성의 응답 조건을 실험 중 바꿉니다. | 200·5xx·timeout·delay 제어 어댑터, 종료 시 정상 모드 복원 |
| 3. 실험 이력 | 🔜 | 그래프의 이상 시점과 주입 조건을 연결합니다. | scenario ID, 시작·종료 시각, 파라미터, 결과를 구조화 로그로 기록 |
| 4. 실행 제어 | 🔜 | 재현 가능한 방식으로 시나리오를 시작·중지합니다. | REST 또는 스케줄 기반 실행 제어, 상태 조회, 중복 실행 방지 |
| 5. 관측 UC 연동 | 🔜 | 스터디 시나리오를 반복 실행하고 증거를 남깁니다. | UC-01~UC-08 실행 절차, `experiments/` 기록, Dashboard·Runbook 교차 링크 |

## 우선 시나리오

| 순서 | 시나리오 | 생성·주입 방식 | 관측 목적 |
|---|---|---|---|
| 1 | 정상 기준선 | 일정 TPS의 유효 이벤트 발행 | 처리량·지연·lag의 정상 범위를 정합니다. |
| 2 | 외부 지연·오류 | WireMock delay, 5xx, timeout | retry·CircuitBreaker·DLT로 번지는 흐름을 확인합니다. |
| 3 | Poison Message | 잘못된 JSON, 누락 필드, 미지원 type | DLT 증가가 외부 API 장애와 다름을 확인합니다. |
| 4 | Consumer Lag | burst 발행과 concurrency 비교 | 외부·DB·JVM·consumer 설정 중 병목을 분기합니다. |
| 5 | Cache Hit/Miss | 동일 또는 매번 다른 `userId` 발행 | 캐시·DB·처리 시간의 관계를 확인합니다. |

JVM 병목은 Runner가 직접 만들기보다, 대량 처리 또는 객체 생성 부하를 조절해 `notification-service`에서 발생하는 heap·GC·CPU 신호를 관측합니다.

## 구현 순서

1. Stage 1에서 정상·burst·poison 이벤트를 Kafka에 발행합니다.
2. Stage 2에서 WireMock의 응답 모드를 변경하고 실험 종료 시 정상 상태로 되돌립니다.
3. Stage 3에서 모든 실행에 scenario ID와 파라미터를 부여해 Loki 로그와 Grafana 주석의 기준점으로 씁니다.
4. Stage 4에서 실행 API 또는 스케줄을 추가하되, 한 시나리오가 중복 실행되지 않도록 보호합니다.
5. Stage 5에서 [관측 UC](../../docs/observability/02-scenarios-and-operations.md)의 재현법·증거·원인 판단을 실제 기록으로 채웁니다.

## 비목표

- 운영 환경의 부하 테스트 도구를 대체하지 않습니다.
- 알림 발송 로직, retry 정책, DLT 재처리는 `notification-service`의 책임입니다.
- Docker Compose·LGMT 구성 자체의 구현은 [관측 스터디 계획](../../docs/observability/00-study-plan.md)에서 관리합니다.
