# notification-scenario-runner — 관측 실험 발생기

`notification-service`에 정상·부하·장애 신호를 전달하는 Phase 3용 Spring Boot 애플리케이션입니다. 현재는 독립 기동 가능한 스캐폴딩만 제공하며, Kafka 발행과 WireMock 제어 기능은 아직 구현하지 않았습니다.

## 책임 경계

- 정상·burst·poison 메시지를 Kafka에 발행합니다.
- WireMock의 200·5xx·timeout·delay 모드를 전환합니다.
- 실행한 시나리오의 시작·종료 시각과 파라미터를 구조화된 Scenario History Log로 남깁니다.

## 실행

```bash
cd ~/notification-lab
docker compose -f infra/compose.yaml up -d kafka wiremock
cd apps/notification-scenario-runner
./gradlew bootRun
```

앱은 기본적으로 8094 포트에서 기동하고 Kafka는 `localhost:9192`를 사용합니다. 전체 관측 설계는 [관측 아키텍처](../../docs/observability/01-architecture.md), 시나리오 목록은 [관측 시나리오와 운영 절차](../../docs/observability/02-scenarios-and-operations.md)를 참고합니다.
