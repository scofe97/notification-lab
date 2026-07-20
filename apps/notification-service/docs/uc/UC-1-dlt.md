# UC-1 대안 흐름 — DLT(Dead Letter Topic) 관찰 노트

이 문서는 UC-1의 실패 경로, 즉 **처리 불가 메시지가 `notification.DLT`에 담긴 뒤 어떻게 되는가**를 손으로 확인하는 노트입니다. UC-1 본류는 [UC-1.md](UC-1.md)에 있습니다.

---

## 먼저 알아야 할 사실 — 담긴 뒤 "자동 처리"는 없다

이 프로젝트는 실패 메시지를 `notification.DLT` 토픽에 **적재만** 합니다. **DLT를 소비하는 리스너는 없습니다.** 그래서 "담긴 뒤 자동 처리"는 **없음**이 정답입니다.

DLT는 "죽은 메시지를 버리지 않고 격리해 두는 보관함"입니다. 그다음 처리(재처리·알림·폐기)는 **사람 또는 별도 소비자의 몫**입니다. 이 경계 — "적재까지가 이 프로젝트 범위, 그다음은 운영 정책" — 를 이해하는 게 DLT 개념의 핵심입니다.

메시지가 DLT로 가는 경로 (코드 근거: `send/config/KafkaConsumerConfig`):
```
리스너에서 예외 → DefaultErrorHandler가 재시도(1초 간격 2회)
  → 소진되면 DeadLetterPublishingRecoverer가 실패 메시지를 {topic}.DLT 로 발행
```

---

## 관찰 1. 실패를 일부러 만들어 DLT에 적재

```bash
# 방법 A) 역직렬화 실패(독약 메시지) — 깨진 JSON 발행
echo 'this-is-not-json' | docker exec -i nlab-kafka \
  /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:9094 --topic notification

# 방법 B) 발송 실패 — WireMock 스텁을 500으로 바꾼 뒤 정상 이벤트 발행
#   infra/wiremock/mappings 에 500 응답 매핑을 추가 → 재시도 2회 소진 → DLT
```
> 앱 로그에서 재시도(1초 간격 2회) 후 `DeadLetterPublishingRecoverer`가 도는 것을 확인합니다.

---

## 관찰 2. DLT에 실제로 쌓였는지 눈으로 확인 (CLI)

```bash
# DLT 토픽이 생겼는지
docker exec nlab-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9094 --list | grep DLT

# DLT 내용을 처음부터 읽기 (실패 메시지가 보존된다)
docker exec nlab-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9094 --topic notification.DLT --from-beginning --timeout-ms 5000 \
  --property print.headers=true
```
- 헤더에 `kafka_dlt-exception-message`, `kafka_dlt-original-topic` 등 **실패 원인**이 붙어 있습니다.
- 이 헤더가 "왜 죽었는가"를 담고 있어, 재처리 판단의 근거가 됩니다.

---

## 관찰 3. GUI로 보고 싶으면 kafka-ui

```bash
cd ~/notification-lab && docker compose -f infra/compose.yaml up -d kafka-ui  # 8100 포트
# http://localhost:8100 → Topics → notification.DLT → 메시지·헤더 확인
```

---

## 확장 — "그다음 처리"를 직접 구현해 보려면 (지금은 안 해도 됨)

- DLT 전용 `@KafkaListener(topics="notification.DLT")`를 하나 더 두고 → 로그만 남기거나 → 조건부로 원래 토픽에 재발행(재처리).
- 실무에선 보통 알림(Slack)·수동 재처리 대시보드·N일 후 폐기 정책과 묶입니다.
- 여기서 목적은 "적재까지가 이 프로젝트 범위, 그다음은 운영 정책"이라는 경계를 이해하는 것입니다.

---

## 확인 기록 (직접 채움)

- [x] 독약 메시지가 재시도 2회 후 DLT로 갔는가: **확인 (2026-07-20)**. JSON이 아닌 문자열을 발행하니 `Record in retry` 로그가 1초 간격으로 2줄 찍히고, `notification.DLT`를 소비했을 때 그 문자열이 그대로 나왔다.
- [x] DLT 헤더에서 실패 원인을 읽었는가: **확인 (2026-07-20)**. `kafka_dlt-exception-cause-fqcn`이 `JsonParseException`, `kafka_dlt-exception-message`에 `Unrecognized token 'hello'`가 실려 있었다. 외부 5xx 케이스는 원인 클래스가 Feign 예외로 바뀐다.
- 막힌 점: 앱 로그에는 재시도 중 예외 스택이 남지 않아(INFO 재시도 알림만), 원인 추적을 DLT 헤더에 의존해야 했다. 또 인프라가 포화 상태일 때는 `/actuator/health`가 200인데도 메시지가 소비되지 않아, health만으로 판단하면 안 된다는 것을 겪었다.
- 새로 안 것: 재시도의 단위가 실패한 안쪽 호출이 아니라 **리스너 메서드 전체**라는 것. 그래서 일부 채널만 실패해도 성공한 채널까지 재발송된다(멱등 처리 부재 시 중복 발송). DLT 재처리는 자동이 아니라 운영 행위이며, **원인이 해소되기 전에 재발행하면 DLT를 왕복만 한다** — 같은 이벤트를 세 번 재처리하고 네 번째에야 성공했다.

> 상세한 실험 절차와 판정 근거는 [학습 문서 Phase 4](../learning/UC-1-kafka-notification.md), 개념 정리는 [Kafka 메시지 처리](../concepts/kafka-message-handling.md)에 있습니다.
