# UC-1. Kafka 알림 발송 — 리뷰·연구 노트

이 문서는 UC-1(Kafka 알림 발송)을 **손으로 리뷰·관찰**하는 노트입니다. UC 명세(주 흐름·대안 흐름·시퀀스)는 [../02-actors-usecases.md](../02-actors-usecases.md) §UC-1에, 설계 근거는 [../03-architecture.md](../03-architecture.md)에 있습니다. 여기서는 **직접 확인하는 절차와 렌즈**만 둡니다.

- **상태**: 구현 완료 + 런타임 E2E 수동 검증 통과 (2026-07-09, [../../PROGRESS.md](../../PROGRESS.md) 참조)
- **관련 FR**: FR-1(소비)·FR-3(캐시)·FR-4(발송)·FR-5(실패 격리)·FR-6(로깅)

---

## 1. 핵심 기술 매핑

이 서비스는 표준 Spring Kafka 구성으로 메시지 처리 추상화를 구현합니다.

| 일반적인 메시지 처리 역할 | Spring Kafka (이 프로젝트) |
|--------------------|----------------------------|
| 토픽 처리 어노테이션 | `@KafkaListener(topics=...)` |
| `MessageHandler<T>` | 리스너 메서드 시그니처 `void handle(T msg)` |
| `FailurePolicy.DLQ` | `DeadLetterPublishingRecoverer` + `DefaultErrorHandler` |
| `FailurePolicy.LOG` | 에러 핸들러에서 로깅 후 skip |
| `TypeMappedMessageProducer` | `KafkaTemplate<K,V>` |

---

## 2. 코드 리뷰 렌즈 — 무엇을 중점으로 볼까

읽는 순서는 **메시지가 흐르는 순서**를 따릅니다: 리스너 → 서비스 → (캐시·회로차단·Feign) → 실패 경로.

| 파일 | 볼 것 (리뷰 렌즈) | 왜 중요한가 |
|------|------------------|-------------|
| `send/listener/NotificationListener` | **예외를 잡지 않는다**(try-catch 없음). 잡으면 어떻게 되나? | 잡아서 삼키면 실패가 성공으로 오프셋 커밋돼 메시지가 사라진다. "안 잡는 게 의도"임을 이해했는지 |
| `application.yml` `enable-auto-commit: false` + `ack-mode: record` | 자동 커밋을 왜 껐나 | 처리 성공해야만 커밋 → 유실 방지(NFR-4). auto-commit이면 처리 전에 커밋될 수 있음 |
| `send/service/NotificationSendService` | 수신자를 **채널별로 그룹핑**하는 로직. 채널 설정(enabled=false)을 어디서 거르나 | 발송의 핵심 분기. 그룹핑 단위가 발송 호출 단위 |
| `@Cacheable(key="#userId + ':' + #channelType")` (`ChannelSettingService`) | 캐시 키 조합이 맞나. 캐시 무효화(`@CacheEvict`)는 있나? | 설정 변경 시 캐시가 안 지워지면 옛 값으로 발송할 수 있음 (지금은 TTL 10분에만 의존 → 리뷰 포인트) |
| `@CircuitBreaker(name="notificationSend")` (서비스 `callSend`) | fallback이 없다. 회로 OPEN이면 무슨 일이? | fallback 미지정 → 예외가 위로 전파 → 리스너 에러 핸들러가 DLT로. 이 연쇄를 코드로 따라갈 수 있는지 |
| `send/config/KafkaConsumerConfig` | 재시도 횟수(2회)·간격(1s)·DLT 토픽명(`{topic}.DLT`) | 재시도 정책이 곧 "몇 번 실패해야 포기하나"의 정의 |
| `send/remote/NotificationSendClient` (Feign) | URL·API 키를 **하드코딩했나** placeholder인가 | 시크릿은 `application.yml` placeholder + 환경변수여야 함 (dev-standards) |

**스스로 던질 질문 3개**:
1. 이 메시지가 처리 도중 죽으면(앱 kill) 다시 소비되나, 유실되나? → 커밋 시점을 따라가 답한다.
2. 같은 (userId, channelType) 설정을 두 번 조회하면 두 번째는 DB를 치나? → 캐시 히트를 로그·`recordStats()`로 확인.
3. WireMock을 500 응답으로 바꾸면 몇 번째 호출에서 회로가 열리나? → `sliding-window-size: 5`, `failure-rate-threshold: 50` 계산해보고 실측과 비교.

---

## 3. 직접 관찰·실측법

### 3-1. 정상 발송 흐름 확인 (E2E 한 번 돌려보기)

```bash
cd ~/notification-lab && docker compose -f infra/compose.yaml up -d  # kafka·wiremock 등
cd apps/notification-service && ./gradlew bootRun  # 8092에 Undertow로 뜬다
# 다른 터미널에서 이벤트 발행 (수신자 2명: SMS·EMAIL)
EVENT='{"eventId":"evt-001","title":"테스트","content":"UC-1","receivers":[{"userId":"user-1","channelType":"SMS","destination":"010-1111-2222"},{"userId":"user-2","channelType":"EMAIL","destination":"u2@example.com"}]}'
echo "$EVENT" | docker exec -i nlab-kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:9094 --topic notification
```
- 앱 로그: `알림 이벤트 수신: eventId=evt-001, 수신자 2명` → `발송 완료: ... [SMS succeeded=1, EMAIL succeeded=1]`
- WireMock 수신 확인: `curl -s http://localhost:8110/__admin/requests/count -X POST -d '{"urlPathPattern":"/send/.*"}'` → count 2

### 3-2. 캐시 히트 관찰 (Caffeine)

```bash
# 같은 (userId, channelType)로 두 번 발송해 2번째가 DB를 안 치는지 본다.
# recordStats()를 켰으니 히트율을 액추에이터로 확인 가능
curl -s http://localhost:8092/actuator/caches 2>/dev/null      # 캐시 목록
# DEBUG 로그로 두 번째 조회 시 DB 쿼리(Hibernate SQL)가 안 나가면 히트
```

### 3-3. 회로차단 관찰 (Resilience4j)

```bash
# WireMock 스텁을 500으로 바꾸고(또는 500 매핑 추가) 반복 발송
curl -s http://localhost:8092/actuator/circuitbreakers 2>/dev/null   # 상태 CLOSED→OPEN 전이
# sliding-window-size=5, failure-rate-threshold=50 → 최근 5건 중 50%(≈3건) 실패면 OPEN
```

### 3-4. 유실 방지 확인 (커밋 시점)

```bash
# 처리 도중 앱을 kill 하고 재기동 → auto-offset-reset=earliest + 미커밋이면 재소비된다
# enable-auto-commit=false + ack-mode=record 이므로 "처리 성공 = 커밋"
```

---

## 4. 확인 기록 (직접 채움)

> 손으로 해보며 예상과 실제가 무엇이 달랐는가를 남깁니다. 아래는 빈 골격입니다.

### FR-1 Kafka 소비
- [ ] 확인:
- 막힌 점:
- 새로 안 것:

### FR-3 채널 설정 캐시 (Caffeine)
- [ ] 확인:
- 막힌 점:
- 새로 안 것:

### FR-4 발송 (OpenFeign + Resilience4j)
- [ ] 확인:
- 막힌 점:
- 새로 안 것:

### FR-5 발송 실패 격리 (회로차단 + DLT)
- [ ] 확인:
- 막힌 점:
- 새로 안 것:

> DLT(실패 메시지가 담긴 뒤 어떻게 처리되는가)는 UC-1의 대안 흐름이지만 관찰 절차가 길어 [UC-1-dlt.md](UC-1-dlt.md)로 분리했습니다.
