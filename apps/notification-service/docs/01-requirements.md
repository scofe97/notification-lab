# notification-service — 요구사항

`notification-service`는 Kafka 알림 이벤트를 소비해 수신자의 채널 설정에 맞춰 발송하고, 이후 이력을 검색·아카이빙하는 서비스입니다. 발송과 이력 흐름은 하나의 Spring Boot 애플리케이션 안에서 패키지로 구분합니다.

액터와 유스케이스는 [02-actors-usecases.md](02-actors-usecases.md), 구조는 [03-architecture.md](03-architecture.md)에서 설명합니다.

## 기능 요구사항

| ID | 기능 | 설명 | 상태 |
|----|------|------|------|
| FR-1 | 알림 이벤트 소비 | Kafka `notification` 토픽의 알림 이벤트를 수신한다 | 구현 완료 |
| FR-2 | 채널별 분류 | 수신자를 SMS·ALIMTALK·EMAIL 채널별로 그룹핑한다 | 구현 완료 |
| FR-3 | 채널 설정 조회 | 수신자의 채널 수신 여부를 캐시해 조회한다 | 구현 완료 |
| FR-4 | 외부 발송 호출 | 채널별 외부 발송 API를 호출한다 | 구현 완료 |
| FR-5 | 발송 실패 격리 | 반복 실패를 재시도한 뒤 DLT로 격리한다 | 구현 완료 |
| FR-6 | 외부 REST 발송 | REST 요청을 채널별 발송 흐름으로 변환한다 | 후속 |
| FR-7 | 메일 실수신 확인 | EMAIL 채널의 발송 결과를 MailHog에서 확인한다 | 후속 |
| FR-8 | 발송 이력 색인 | 발송 결과를 OpenSearch에 색인한다 | 후속 |
| FR-9 | 이력 ID 생성 | 이력에 시간순 정렬 가능한 ULID를 부여한다 | 후속 |
| FR-10 | 알림 이력 조회 | 채널·기간으로 발송 이력을 검색한다 | 후속 |
| FR-11 | 로그 아카이빙 | 전일 이력을 파일로 내보낸다 | 후속 |
| FR-12 | 알림채널 설정 CRUD | 사용자의 알림 수신 여부를 조회·저장한다 | 후속 |

> FR-1~5가 관측 대상이 되는 발송 파이프라인입니다. 나머지 기능은 2단계에서 확장합니다.

## 비기능 요구사항

| ID | 항목 | 요구 |
|----|------|------|
| NFR-1 | 런타임 | Java 21과 Spring Boot 3.5.x를 사용한다 |
| NFR-2 | 웹서버 | Tomcat 대신 Undertow로 기동한다 |
| NFR-3 | 발송 대체 | WireMock과 MailHog로 외부 발송을 검증한다 |
| NFR-4 | 메시지 유실 방지 | 소비·발송 실패는 재시도 후 DLT로 격리한다 |
| NFR-5 | 시크릿 | 목 API 키·URL은 환경변수로 주입한다 |
| NFR-6 | 데이터 | 채널 설정·발송 이력은 PostgreSQL에 저장한다 (2026-07-22 H2에서 전환). OpenSearch 색인은 후속 후보 |
| NFR-7 | 검색 | 로컬 OpenSearch single-node에서 이력을 조회한다 |
| NFR-8 | SSL 안전성 | SSL 검증 우회는 로컬 학습 대상으로만 제한하고 위험성을 문서화한다 |

## 기술 매핑

| 관심사 | 이 프로젝트의 구현 | 학습 초점 |
|--------|------------------|-----------|
| Kafka 소비 | `@KafkaListener` | 토픽 구독, 역직렬화, 오프셋 커밋 |
| 실패 정책 | `DefaultErrorHandler` + DLT | 재시도와 실패 메시지 격리 |
| 회로차단 | Resilience4j `@CircuitBreaker` | 반복 실패 시 외부 호출 차단 |
| 채널 설정 캐시 | Caffeine `@Cacheable` | 조회 부하와 cache hit/miss |
| 선언적 HTTP | OpenFeign | 인터페이스 기반 HTTP 클라이언트 |
| 외부 발송 | WireMock + MailHog | 성공·5xx·timeout·delay 재현 |
| 이력 | OpenSearch + ULID | 색인·조회·시간순 식별자 |

## 범위 밖

- HMAC 기반 발송 서명
- Avro와 Schema Registry
- 프론트엔드 화면
- 실제 클라우드 발송 인프라
