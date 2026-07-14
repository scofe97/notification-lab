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

- [ ] 독약 메시지가 재시도 2회 후 DLT로 갔는가:
- [ ] DLT 헤더에서 실패 원인을 읽었는가:
- 막힌 점:
- 새로 안 것:
