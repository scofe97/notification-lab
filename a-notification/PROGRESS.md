# a-notification — 진행 상태

이 문서는 구현 진행 지점·빌드 상태·중단 지점을 기록합니다. 프로젝트 개요는 [README.md](README.md), 개념 학습 기록은 [NOTES.md](NOTES.md), 설계(요구·유스케이스·아키텍처)는 [docs/](docs/)에 있습니다.

**현재 상태**: UC-1 발송 파이프라인 완성 + 런타임 E2E 수동 검증 통과 (2026-07-09) = **Phase 1 완료**. 단계(Phase) 정의와 다음 국면(LGMT Observability)은 [../ROADMAP.md](../ROADMAP.md)가 SSOT입니다.

---

## 완료

### 스캐폴딩
- `build.gradle` — Java 21 toolchain, Spring Boot 3.5.0 + Undertow(Tomcat 제외), Spring Kafka, OpenFeign, Resilience4j, Caffeine, JPA+H2
- `settings.gradle`, Gradle wrapper 8.10.2
- `NotificationApplication` — `@EnableFeignClients`·`@EnableCaching`
- `application.yml` — 앱 포트 8092, Kafka `localhost:9192`
- `.gitignore`
- `./gradlew compileJava` → **BUILD SUCCESSFUL**

### 인프라 (`~/notification-lab/infra/docker-compose.yml` — 2026-07-13 루트에서 infra/로 이관, 프로젝트명 `notification-lab` 고정)
- apache/kafka:3.8.0 · redis · opensearch · mailhog · wiremock · kafka-ui — 기동 확인됨
- Kafka 포트 **9192** (9092는 다른 학습 프로젝트 redpanda-local이 점유 → 회피)
- WireMock 발송 성공 스텁: `~/notification-lab/infra/wiremock/mappings/send-success.json`

### UC-1 발송 파이프라인 (컴파일 통과) — 전부 `notification.send` 하위
- `send/domain`: `ChannelType` · `NotificationEvent` · `SendResult`
- `send/remote`: `NotificationSendClient`(Feign) · `SendRequest` · `SendResponse`
- `send/channel`: `ChannelSetting`(복합키 Entity) · `ChannelSettingRepository` · `ChannelSettingService`(`@Cacheable`)
- `send/service`: `NotificationSendService`(채널 그룹핑 + `@CircuitBreaker` 발송)
- `send/listener/NotificationListener` — `@KafkaListener`로 `notification` 토픽 소비 → JSON 역직렬화 → `send()` 호출. 예외를 잡지 않음(에러 핸들러가 DLT로)
- `send/config/KafkaConsumerConfig` — `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`(실패 → `notification.DLT`), 재시도 2회·1초 간격
- `send/config/CacheConfig` — Caffeine(TTL 10분·최대 10,000)

---

## UC-1 런타임 검증 결과 (수동 E2E, 2026-07-09)

`docker compose up -d` 후 `./gradlew bootRun` → `notification` 토픽에 수신자 2명(SMS·EMAIL) 이벤트 발행:

- `Undertow started on port 8092` — Undertow(Tomcat 대체) 기동
- `partitions assigned: [notification-0]` — `@KafkaListener` 컨슈머 그룹 조인·파티션 할당
- `알림 이벤트 수신: eventId=evt-001, 수신자 2명` — 리스너 수신·역직렬화
- `발송 완료: ... [SMS succeeded=1, EMAIL succeeded=1]` — 채널 그룹핑 후 발송
- WireMock `/send/sms`·`/send/email` 각 1건 수신(요청 카운트 2) — Feign(회로차단 경유) 실제 호출 도달

> 부팅 직후 `NotCoordinatorException`(INFO) 몇 줄은 KRaft 브로커의 그룹 코디네이터 선출 지연으로, `partitions assigned`로 자동 해소됨(실패 아님).

---

## 남은 것 (Phase별 — [ROADMAP](../ROADMAP.md) 순서)

**Phase 2 — 축 A 마무리**
1. E2E 테스트(Testcontainers) 자동화 — Kafka 발행 → WireMock 발송 검증, 실패→회로차단+DLT, 캐시 히트. 위 수동 검증을 자동 테스트로 박제 (Task #17, Phase 2-1)
2. UC-4 채널 설정 REST CRUD (Phase 2-2) · UC-2 외부 REST 발송 (Phase 2-3)
3. 이력(`notification.history`) 패키지 — UC-3(조회)·UC-5(아카이빙)·SSL 재현 (Phase 2-4)

**Phase 3 — LGMT Observability 스터디 (다음 국면, 1개월)**
- 이 서비스를 관측 대상으로 Loki·Grafana·Mimir·Tempo 도입 + 장애 실험 5종. 주차별 계획·간극 표는 [ROADMAP Phase 3](../ROADMAP.md) 참조. 목표 아키텍처는 [docs/03-architecture.md](docs/03-architecture.md) §9

**Phase 4 — 축 B(사내 CMP 플랫폼)**: 보류

---

## 재개 절차

```bash
cd ~/notification-lab
docker compose -f infra/docker-compose.yml up -d   # 인프라 먼저 (kafka·wiremock 등)
cd a-notification
./gradlew bootRun             # 위 4개 만든 뒤
```

> 중단 이유: "스캐폴딩만" 방침에 따라 로직 구현 중 멈춤. 만든 로직 파일은 보존(컴파일 통과 상태).

## 알려진 이슈·결정

- **Kafka 포트 9092 충돌**: redpanda-local 컨테이너가 점유 → notification-lab는 9192 사용 (compose·application.yml 양쪽 반영)
- **bitnami/kafka:3.6.1 매니페스트 없음**: Bitnami 태그 정책 변경 → 공식 `apache/kafka:3.8.0`으로 교체
- **Java 25 금지**: 시스템 기본이 25지만 Gradle toolchain으로 21 강제 (Temurin/Corretto 21 설치돼 있음)
- **Kafka 헬스체크 무한 starting**: 헬스체크가 EXTERNAL 리스너(`localhost:9092`→advertise `localhost:9192`)로 붙어, 컨테이너 내부에서 `9192`가 안 열려 실패. 내부 리스너 `kafka:9094`로 변경해 해결. (호스트/앱은 그대로 `localhost:9192` 사용)
