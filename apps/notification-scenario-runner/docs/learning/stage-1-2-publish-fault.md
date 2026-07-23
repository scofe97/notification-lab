# 단계 1·2 · 발행과 장애 제어 — CLI 원샷, 동기 발행, poison 3변형

> 관측 실험의 신호(정상·burst·독약)와 장애(5xx·지연)를 명령 한 줄로 만드는 도구를 학습 단위로 다룹니다. Runner는 신호를 만들 뿐 해석하지 않는다는 경계, 그리고 원샷 앱이라는 형태가 설계를 어떻게 바꿨는지가 관전 포인트입니다.

- 근거: 구현 커밋 `4b4cfad`, 2026-07-23 E2E 스모크, [runner ROADMAP](../../ROADMAP.md)
- 연결: [본체 UC-1 학습 문서](../../../notification-service/docs/learning/UC-1-kafka-notification.md)(이 신호를 소비하는 쪽) · [관측 시나리오](../../../../observability/docs/02-scenarios-and-operations.md)(이 도구가 쓰일 실험들) · [컨벤션](../../../../AGENTS.md)



## 구현 개요 — 먼저 읽는 지도

구현 범위는 둘입니다. ① 시나리오 타입별 Kafka 발행(normal·burst·poison), ② WireMock 장애 주입·복원(fault-5xx·fault-delay·fault-reset). 트리거는 CLI 원샷 — `--scenario` 인자로 한 번 실행하고 앱이 종료됩니다(상주형 REST 제어는 단계 4).

| 구성요소 | 파일 | 역할 |
|---|---|---|
| CLI 어댑터 | `api/ScenarioCommandLineRunner.java` | args 파싱(기본값 보정) → in-port 1회 호출. main이 완료 후 exit |
| 유스케이스 구현 | `application/ScenarioService.java` | 타입별 분기 — 발행 계열은 EventPublishPort, 장애 계열은 FaultInjectionPort로 위임 |
| 도메인 모델 | `domain/model/ScenarioType·ScenarioRequest.java` | 6타입 enum + 순수 요청 record |
| in-port | `domain/port/in/RunScenarioUseCase.java` | 실행 진입 계약 |
| out-port 2개 | `domain/port/out/EventPublishPort·FaultInjectionPort.java` | 발행 요구 / 장애 전환 요구 |
| Kafka 어댑터 | `infrastructure/kafka/KafkaEventPublishAdapter.java` | NotificationEvent 호환 JSON 조립, **동기(get) 발행**, poison 3변형 |
| WireMock 어댑터 | `infrastructure/wiremock/WireMockFaultAdapter.java` | admin API로 priority 1 스텁 주입·reset |

검증 상태: normal→이력 적재, poison 3변형→DLT, fault-5xx→DLT 격리, fault-reset→200 복원은 스모크로 확인. fault-delay·burst는 아직 실행으로 보지 않았습니다(아래 표).



## 증거 등급

**확인됨**: 실행해서 관찰했다 · **코드상 추론**: 코드·설정상 필연이지만 실행으로 보진 않았다 · **미검증**: 코드를 읽어도 확정할 수 없다.

| 주장 | 등급 | 근거 |
|---|---|---|
| normal 발행이 본체 파이프라인을 관통해 이력에 적재된다 | 확인됨 | 2026-07-23 스모크 — 이력에 `scn-*` 3건 |
| poison 3변형이 전부 DLT로 격리된다 | 확인됨 | 스모크 — DLT +3 |
| fault-5xx 주입 후 발송은 재시도 끝에 DLT로 간다 | 확인됨 | 스모크 — DLT `scenario-runner` 계열 +1 |
| fault-reset이 파일 스텁 기본값(200)으로 복원한다 | 확인됨 | 스모크 — reset 후 발송 스텁 200 |
| 동기(get) 발행이라 원샷 종료 시에도 미전송 유실이 없다 | 코드상 추론 | `send().get(10s)` — 비동기+즉시종료 조합의 유실은 재현해 보지 않음 |
| fault-delay가 지정 채널 응답을 지연시킨다 | **결함 발견 → 수정 후 확인됨 (2026-07-23)** | 1차 실측에서 본체 `DecodeException`→재시도→CB OPEN. 원인은 스텁의 Content-Type 누락(WireMock이 octet-stream 응답). 헤더 추가(`33d3a37`) 후 재실측 — 수신→발송 완료 3.07초, 재시도 없음, 1/1 성공 |
| Kafka 다운 시 runner는 fail-fast한다 (삼키지 않음) | **확인됨 (2026-07-23)** | 브로커 정지 후 `--count=3` → **0건 발행**, `TimeoutException`, exit code 1 |
| 발행 실패 판정까지 60초가 걸린다 | **확인됨 (2026-07-23)** | 같은 실험 — `Topic not present in metadata after 60000 ms` = producer `max.block.ms` 기본값 |
| burst가 간격 없이 발행돼 lag을 만든다 | 코드상 추론 | intervalMs=0 강제 — lag 관측은 Phase 3 계측 이후에나 가능 |



## 후속 검증

| 항목 | 상태 | 확인 방법 |
|---|---|---|
| fault-delay 실측 | ✅ 확인됨 (2026-07-23) | 결함 수정 후 3.07초 지연 관찰 — 위 증거 등급 표 |
| Kafka 다운 시 runner 동작 | ✅ 확인됨 (2026-07-23) | 0건·exit 1·60초 — 위 증거 등급 표 |
| burst → lag 실측 | 대기 | Phase 3 계측(consumer lag 메트릭) 이후 UC-06에서 |
| 발행 실패 대기 시간 단축 | 대기 | `max.block.ms` 축소 검토 — 실험 도구가 60초씩 멈추면 반복 실행이 느려짐 |
| 나머지 스텁의 Content-Type | 대기 | 5xx 스텁은 예외 경로라 무사했으나, 향후 200 응답을 모사하는 스텁은 헤더 필수 — 신규 스텁 추가 시 점검 |



## Phase 진행 기록

> 각 Phase는 대화로 진행하고, 여기에는 통과 여부와 해결된 오해만 남깁니다. 개인 답변 원문은 기록하지 않습니다.

- [x] Phase 1 · 맥락과 예측 — 원샷 앱의 동기 발행 필요성을 자력 도출. poison 3변형의 터지는 위치는 교정(FAX는 라우팅이 아니라 역직렬화) (2026-07-23)
- [x] Phase 2 · 안내된 흐름 읽기 — 포트 3종·유스케이스 전체 리뷰. poison 페이로드 조립을 어댑터에 둔 배치를 "전송 형식은 어댑터 소관"으로 판단 (2026-07-23)
- [x] Phase 3 · 실패·경계 추적 — Kafka 다운 시 0건·비정상 종료를 추론으로 도출 (2026-07-23)
- [x] Phase 4 · 실측 실습 — **delay 스텁 결함 발견·수정·재검증** + fail-fast 60초 실측 (2026-07-23)
- [x] Phase 5 · 능동 인출 — 정식 5문항 (2026-07-23). 자력: 삼킴/던짐 3배치 중 2건·원샷 발행 방식·**포트 문패 기준(4회 만에 정착)**

해결된 오해: ① poison 3변형 중 변환을 통과하는 것은 receivers 누락뿐 — enum 매핑(FAX)은 역직렬화의 일부라 그 안에서 죽는다 ② 이력 기록을 삼키는 이유는 "재시도가 없어서"가 아니라 "부가 관심사라 주 관심사(발송)를 취소시키면 안 되어서" ③ 인바운드 어댑터가 REST일 필요는 없다 — 이 앱에선 CLI가 그 자리(단계 4에서 REST가 같은 in-port에 추가).

앵커 문장(다음 세션 첫 복습): ① 앱의 수명이 발행 방식을 결정한다 ② 부가 관심사의 실패는 삼키고, 주 관심사의 실패는 크게 알린다 ③ 형식이 멀쩡한 불량품이 제일 멀리 들어온다 ④ 클라이언트의 기본 대기(Hikari 30초·Kafka 60초)를 모르면 실패가 언제 드러나는지도 모른다 ⑤ 성공 모사가 실패 모사보다 까다롭다 — 성공 응답은 본문까지 읽히니까.
