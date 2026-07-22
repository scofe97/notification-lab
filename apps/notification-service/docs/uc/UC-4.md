# UC-4. 알림채널 설정 — 리뷰·연구 노트

사용자가 개인 알림 수신 채널(SMS/Email/알림톡)을 켜고 끄는 흐름입니다. UC 명세는 [../02-actors-usecases.md](../02-actors-usecases.md) §UC-4에 있습니다.

- **상태**: 구현 + 스모크 검증 완료 (2026-07-21). 헥사고날 `channel` 컨텍스트(api·application·domain·infrastructure)로 GET/PUT 계약과 캐시 갱신을 구현 — 커밋 `ab36f0d`(REST+캐시), `0173527`(헥사고날 분리). 상세 실측은 [학습 문서](../learning/UC-4-channel-setting.md).
- **관련 FR**: FR-12
- **주 액터**: 사용자 (FE)
- **학습 순서**: [UC-1](UC-1.md)과 상호 — UC-4가 저장한 설정을 UC-1 발송 파이프라인이 읽는다. 번호는 명세 ID이며 진행 순서의 SSOT는 [ROADMAP](../../../../ROADMAP.md)이다.
- **미니 방침**: 프론트 최소화 — REST 계약(요청/응답)만, 화면은 만들지 않음

---

## 1. 설계 초점

| 기술 | UC-4에서의 역할 |
|------|----------------|
| JPA 복합키 | (userId, channelType) 단위 설정 저장 (`channel/infrastructure/persistence/ChannelSettingEntity`) |
| 헥사고날 4계층 | api·application·domain·infrastructure로 나눠 도메인이 프레임워크를 모른다 |
| `@CachePut` 캐시 갱신 | 저장 시 캐시를 새 값으로 덮어써 발송 파이프라인에 즉시 반영 |

---

## 2. 코드 리뷰 렌즈

**UC-1과의 연결**이 핵심입니다: UC-4가 저장하는 `ChannelSetting`을 UC-1의 발송 파이프라인(FR-3)이 **읽어서** enabled=false인 채널을 거릅니다. 즉 UC-4는 UC-1의 입력 데이터를 만드는 쪽.

- 볼 것: GET(조회)/PUT(저장) 계약, 저장 시 캐시 갱신을 어떻게 하는가. 실제 구현은 `@CacheEvict`(지우기)가 아니라 **`@CachePut`(새 값 써넣기)** 다 (`channel/application/ChannelSettingService.java:56`). 저장 시점에 정확한 값을 이미 알므로 지우고 다음 미스를 유발하는 대신 캐시에 바로 써 넣는다 — 코드 주석(48~54행)이 이 선택 이유를 담는다. 키는 `isEnabled`의 `@Cacheable` 키와 반드시 같은 형식이어야 하며, 다르면 갱신이 아니라 별도 항목이 생겨 무효화가 조용히 실패한다.
- 이 `@CachePut` 선택이 UC-1 리뷰 렌즈의 "캐시 무효화 없음" 지적을 해소한다.

---

## 3. 직접 관찰·실측법

- PUT으로 설정을 끈 직후 UC-1 발송을 돌려, 캐시가 즉시 갱신돼 새 설정이 반영되는지 확인 — TTL 만료를 기다리지 않는다.
- 실측(2026-07-21 스모크): PUT 수신거부 후 재발행 시 WireMock 도달 수가 정지하고 "발송 대상 없음" 로그가 찍혀, TTL 대기 없이 즉시 반영됨을 확인. 상세는 [학습 문서 증거 등급 표](../learning/UC-4-channel-setting.md).
- 남은 실측: enabled 누락 PUT의 400 응답, 캐시 키 불일치 시나리오 — [학습 문서 후속 검증](../learning/UC-4-channel-setting.md) 참조.

---

## 4. 확인 기록 (직접 채움)

- [ ] 확인:
- 막힌 점:
- 새로 안 것:
