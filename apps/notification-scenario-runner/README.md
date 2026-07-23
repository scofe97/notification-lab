# notification-scenario-runner — 관측 실험 발생기

`notification-service`에 정상·부하·장애 신호를 전달하는 3단계용 Spring Boot 애플리케이션입니다. 단계 1(Kafka 발행)·단계 2(WireMock 장애 제어)가 구현돼 있고, 실험 이력(단계 3)·상주형 REST 제어(단계 4)는 3단계 진행 중 추가합니다.

## 책임 경계

- 정상·burst·poison 메시지를 Kafka에 발행합니다. (단계 1 ✅)
- WireMock의 200·5xx·delay 모드를 전환하고 정상으로 복원합니다. (단계 2 ✅)
- 실행한 시나리오의 시작·종료 시각과 파라미터를 구조화된 Scenario History Log로 남깁니다. (단계 3 🔜)

## 실행 — CLI 원샷

단계 4(REST 제어) 전까지는 `--scenario` 인자로 한 번 실행하고 종료하는 방식입니다.

```bash
cd ~/notification-lab
docker compose -f infra/compose.yaml up -d kafka wiremock
cd apps/notification-scenario-runner

# 정상 기준선 — 10건을 500ms 간격으로 (관측 UC-01)
./gradlew bootRun --args="--scenario=normal --count=10 --interval-ms=500 --channel=sms"

# burst — 간격 없이 연속 발행 (관측 UC-06 consumer lag)
./gradlew bootRun --args="--scenario=burst --count=50"

# 독약 3변형 순환 — JSON 아님 / receivers 누락 / 미지원 채널 (관측 UC-02 DLT)
./gradlew bootRun --args="--scenario=poison --count=3"

# 외부 장애 주입·복원 (관측 UC-03·04)
./gradlew bootRun --args="--scenario=fault-5xx --channel=sms"
./gradlew bootRun --args="--scenario=fault-delay --channel=email --delay-ms=3000"
./gradlew bootRun --args="--scenario=fault-reset"   # 실험 종료 시 반드시 실행
```

기본값: `count=5`, `interval-ms=500`, `channel=sms`, `delay-ms=3000`. Kafka는 `localhost:9192`(토픽 `notification`), WireMock admin은 `localhost:8110`이며 환경변수(`KAFKA_BOOTSTRAP`·`SCENARIO_TOPIC`·`SCENARIO_WIREMOCK_URL`)로 바꿉니다. 구조는 [AGENTS.md](../../AGENTS.md) 헥사고날 컨벤션(api/application/domain+port/infrastructure)을 따릅니다. 전체 관측 설계는 [관측 아키텍처](../../observability/docs/01-architecture.md), 시나리오 목록은 [관측 시나리오와 운영 절차](../../observability/docs/02-scenarios-and-operations.md)를 참고합니다.
