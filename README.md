# notification-lab — Kafka Notification Observability Lab

Kafka 알림 파이프라인에서 지연·실패·재시도·DLT·캐시·DB 병목을 의도적으로 만들고, LGMT로 원인을 추적하는 관측 실험실입니다. 알림 서비스는 관측 대상이고, 실험 결과와 Runbook이 이 저장소의 핵심 결과물입니다.

## 프로젝트 구성

```text
notification-lab/
  OVERVIEW.md                       # 목적·통합 아키텍처·기능별 학습 여정 진입점
  ROADMAP.md                        # 단계와 3단계 관측 스터디 SSOT
  apps/                             # 코드
    notification-service/           # Kafka 소비·채널 설정·외부 발송 관측 대상
    notification-scenario-runner/   # 🟡 3단계 부하·장애 발생기 스캐폴딩
  infra/                            # Kafka·WireMock·LGMT compose와 설정
  observability/                    # 3단계 관측 스터디 (설계 + 결과물 한 지붕)
    docs/                           # 관측 설계 문서 (계획·아키텍처·시나리오)
    dashboards/                     # Grafana dashboard JSON
    alerts/                         # alert rule
    scenarios/                      # 반복 가능한 시나리오 정의
    experiments/                    # 증거와 원인 판단 기록
    runbooks/                       # 장애 조사 절차
```

## 현재 상태

`apps/notification-service`는 Kafka `notification` 토픽을 소비하고, Caffeine으로 채널 설정을 조회한 뒤 OpenFeign과 CircuitBreaker를 거쳐 WireMock 발송 API를 호출합니다. 실패는 현재 인메모리에서 2회 재시도한 뒤 `notification.DLT`로 격리합니다.

1단계의 수동 E2E 검증은 완료했습니다. 이후 순서와 관측 스터디의 설계는 [ROADMAP.md](ROADMAP.md)에서 관리합니다.

## 실행

```bash
cd ~/notification-lab
docker compose -f infra/compose.yaml up -d
cd apps/notification-service
./gradlew bootRun
```

서비스 설계와 UC별 검증 노트는 [notification-service 문서](apps/notification-service/README.md), 발생기 책임 경계는 [scenario-runner 문서](apps/notification-scenario-runner/README.md)에서 확인합니다.
