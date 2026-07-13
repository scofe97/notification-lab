# 축 A 원본 알림 서비스 — 요구사항

이 문서는 원본 알림 서비스(사내 알림 서비스)를 오픈소스로 축소 재현하는 **축 A 전체의 요구사항**입니다. 코드는 학습 편의상 `a1-notification-core`(발송 파이프라인)와 `a2-notification-dispatch`(발송 확인·아카이빙)로 나누지만, 요구사항은 하나의 서비스로 봅니다.

- 원본 근거: `비공개 분석 노트/원본 커스텀/01-backend.md` §1(기능)·§5(유스케이스 흐름)
- 액터·유스케이스 상세는 [02-actors-usecases.md](02-actors-usecases.md), 구조는 [03-architecture.md](03-architecture.md) 참조

---

## 1. 서비스 한 줄 정의

**CMP 본체가 Kafka로 발행한 알림 이벤트를 소비해, 수신자의 채널 설정(SMS·알림톡·이메일)에 따라 분기 발송하고, 그 이력을 기록·조회하는 서비스**입니다. 실제 원본은 발송을 NCP SENS로 하지만, 이 미니 프로젝트는 클라우드를 쓰지 않으므로 발송 대상을 오픈소스 목(WireMock·MailHog)으로 대체합니다.

---

## 2. 기능 요구사항 (FR)

원본 §5의 세 흐름(Kafka 발송·외부솔루션 REST 발송·이력 조회)과 §1의 부가 기능(채널 관리·로그 수집)에서 도출했습니다.

| ID | 기능 | 설명 | 원본 대응 | 담당 프로젝트 |
|----|------|------|-----------|--------------|
| **FR-1** | 알림 이벤트 소비 | Kafka `notification` 토픽을 구독해 알림 이벤트를 수신한다 | §5.1 `NotificationTopicHandler` | a1 |
| **FR-2** | 채널별 분류 | 수신자를 SMS / ALIMTALK / EMAIL 채널 타입별로 그룹핑한다 | §5.1 ⑤ 그룹핑 | a1 |
| **FR-3** | 채널 설정 조회 (캐시) | 수신자별 채널 수신 여부·설정을 조회한다. 자주 안 바뀌므로 캐시한다 | §1 채널 관리 + Caffeine | a1 |
| **FR-4** | 외부 발송 호출 | 채널별로 외부 발송 API(SMS/메일)를 호출한다 | §5.1 ⑦ `NcpUtil` HTTPS | a1 |
| **FR-5** | 발송 실패 격리 | 발송 실패가 반복되면 회로를 차단하고, 처리 불가 메시지는 DLT로 보낸다 | 사내 Kafka 라이브러리 `FailurePolicy.DLQ` | a1 |
| **FR-6** | 외부 솔루션 REST 발송 | REST 요청을 받아 CMP 조직 API로 수신자를 조회한 뒤 채널별로 발송·집계한다 | §5.2 | a1(후속) |
| **FR-7** | 메일 실수신 확인 | EMAIL 채널은 MailHog로 실제 전송해 수신함에서 확인한다 | §5.1 메일 발송 | a2 |
| **FR-8** | 발송 이력 색인 | 발송 결과(수신자·채널·성공여부·시각)를 OpenSearch에 색인한다 | §1 OpenSearch 수집 | a2 |
| **FR-9** | 이력 ID 생성 | 각 이력에 ULID를 부여한다 (시간순 정렬 가능) | ULID 개념(사내 CMP 플랫폼) | a2 |
| **FR-10** | 알림 이력 조회 | 채널·기간으로 발송 이력을 검색한다 | §5.3 | a2 |
| **FR-11** | 로그 아카이빙 배치 | 스케줄러가 주기적으로 전일 이력을 파일로 내보낸다 | §1 매일 00:05 배치 | a2 |
| **FR-12** | 알림채널 설정 CRUD | 사용자가 개인 알림채널(SMS/Email/알림톡 수신 여부)을 조회·저장한다 | §6 `NotificationChannelController` | a1 (REST 최소) |

> **우선순위**: FR-1~5가 핵심(a1의 발송 파이프라인). FR-6~12는 주변부로, a1 완성 후 a2에서 진행합니다. FR-12는 프론트가 호출하던 것이지만 프론트는 최소화하므로 REST 계약만 둡니다.

---

## 3. 비기능 요구사항 (NFR)

| ID | 항목 | 요구 | 근거 |
|----|------|------|------|
| **NFR-1** | 런타임 | Java 21 / Spring Boot 3.5.x | 원본 실제 스택(§2). Java 25 금지 |
| **NFR-2** | 웹서버 | Undertow (Tomcat 제외) | 원본이 Undertow 사용 — 교체 재현 |
| **NFR-3** | 발송 대체 | 실제 NCP 대신 WireMock(SMS/알림톡)·MailHog(메일)로 발송 검증 | 클라우드 미사용 원칙 |
| **NFR-4** | 메시지 유실 방지 | 소비·발송 실패 시 메시지를 잃지 않는다(재시도 후 DLT) | Kafka 신뢰성 소비 |
| **NFR-5** | 시크릿 | 목 API 키·URL은 하드코딩 금지, `application.yml` placeholder + 환경변수 | dev-standards |
| **NFR-6** | 데이터 | H2 파일모드 (채널 설정·메타). 이력은 OpenSearch | 원본 로컬도 H2 |
| **NFR-7** | 검색 | OpenSearch single-node (Docker) | §2 인프라 |
| **NFR-8** | SSL 재현 안전장치 | SSL 검증 우회는 재현하되 위험성 주석 필수, 로컬 목 대상으로만 | ssl.md 안티패턴 학습 |

---

## 4. 개념 재현 매핑 (학습 초점)

이 서비스로 체득하는 개념과, 원본 실제 → 오픈소스 대체입니다. 상세는 각 개념 분석 문서(`concepts/`)로 링크됩니다(NOTES.md 참조).

| 개념 | 원본 실제 | 이 프로젝트에서 | 배우는 것 |
|------|-------------|-----------------|-----------|
| Kafka 소비 | 사내 Kafka 라이브러리 `@TopicHandler` | `@KafkaListener` | 사내 추상화가 감춘 표준 API |
| 실패 정책 | 사내 Kafka 라이브러리 `FailurePolicy.DLQ` | `DeadLetterPublishingRecoverer` + DLT | 실패 메시지를 잃지 않고 격리 |
| 회로차단 | (원본엔 미적용) | Resilience4j `@CircuitBreaker` | 반복 실패 시 외부 호출 차단 |
| 채널 설정 캐시 | (사내 CMP 플랫폼 Caffeine 개념) | Caffeine `@Cacheable` | 조회 부하 감소, TTL·최대크기 |
| 선언적 HTTP | OpenFeign(단 `@Deprecated`) | OpenFeign | 인터페이스만으로 HTTP 클라이언트 |
| 내장 웹서버 | Undertow | Undertow | starter에서 Tomcat 제거·교체 |
| 외부 발송 | NCP SENS SMS·알림톡·메일 | WireMock + MailHog | 발송 결과를 실제로 관측 |
| 로그 아카이빙 | OpenSearch 인덱스 → 파일 | OpenSearch 색인 + 배치 | 검색엔진 색인·조회·스케줄 |
| 이력 ID | (사내 CMP 플랫폼 ULID) | ULID | UUID 대비 정렬 가능한 ID |
| SSL 우회 | `SslConfig` 전역 비활성화 | 동일 재현 + 위험 주석 | **왜 위험한가**(MITM) |

> 원본의 실제 발송은 `NcpUtil`의 직접 HTTP(HMAC-SHA256 서명)이고 OpenFeign 클라이언트(`NotificationApiClient`)는 URL 인코딩 이슈로 `@Deprecated`입니다. 이 프로젝트는 학습을 위해 OpenFeign을 정상 경로로 되살려 선언적 HTTP를 체득하고, HMAC 서명은 목 API라 범위 밖으로 둡니다.

---

## 5. 범위 밖 (안 하는 것)

- **HMAC-SHA256 발송 서명** — 목 API라 불필요. 문서로만 이해
- **Avro + Schema Registry** — 1차는 String/JSON 직렬화(원본도 실제 String). 여력 시 추가
- **Harbor 아티팩트 수집** — 알림 본류와 무관, 생략
- **프론트엔드** — 최소화. FR-12는 REST 계약만, 이벤트는 테스트에서 직접 Kafka로 발행
