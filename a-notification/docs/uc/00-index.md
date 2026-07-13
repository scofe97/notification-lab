# UC별 리뷰·연구 노트 — 인덱스

이 폴더는 UC마다 한 문서를 두고 **구현·리뷰·연구를 손으로 확인하는 절차**를 기록합니다. UC 명세(주 흐름·시퀀스)는 상위 [../02-actors-usecases.md](../02-actors-usecases.md), 설계는 [../03-architecture.md](../03-architecture.md), 요구사항은 [../01-requirements.md](../01-requirements.md)에 있습니다.

각 UC 문서 구성: **① 개념 역링크 → ② 코드 리뷰 렌즈 → ③ 직접 관찰·실측법 → ④ 확인 기록(직접 채움)**.

## 문서 목록 · 구현 현황판

구현 범위 열의 클래스는 전부 `src/main/java/com/practice/notification/send/` 아래에 있습니다. Phase 번호는 [../../../ROADMAP.md](../../../ROADMAP.md)의 단계입니다.

| 문서 | UC | 상태 | 핵심 기술 | 구현 범위 (소스) | 검증 | 다음 작업 |
|------|-----|------|-----------|------------------|------|-----------|
| [UC-1.md](UC-1.md) | Kafka 알림 발송 | **구현+검증 완료** | @KafkaListener·Caffeine·OpenFeign·Resilience4j | `send` 전 패키지 — listener/`NotificationListener` · service/`NotificationSendService` · channel/`ChannelSettingService` · remote/`NotificationSendClient` · config/`KafkaConsumerConfig`·`CacheConfig` · domain 3종 | 수동 E2E 통과 (2026-07-09) | E2E 자동화 — Testcontainers (Phase 2-1) |
| [UC-1-dlt.md](UC-1-dlt.md) | └ 실패 경로(DLT 관찰) | 적재까지 구현 | DeadLetterPublishingRecoverer | config/`KafkaConsumerConfig` (재시도 2회 → `notification.DLT`) | 미실측 (적재 경로 구현만) | DLT 적재 실측. 재처리 실험은 Phase 3 후보 |
| [UC-2.md](UC-2.md) | 외부 솔루션 REST 발송 | 미구현 | OpenFeign(조직 API) | 없음 | — | REST 진입점 → 기존 발송 서비스 재사용 (Phase 2-3) |
| [UC-3.md](UC-3.md) | 알림 이력 조회 | 미구현 | OpenSearch·채널별 매퍼 | 없음 (`history` 패키지 미생성) | — | OpenSearch 색인·조회 (Phase 2-4) |
| [UC-4.md](UC-4.md) | 알림채널 설정 | 데이터만 존재 | JPA 복합키·REST CRUD | channel/`ChannelSetting`·`ChannelSettingRepository`·`ChannelSettingService` — 데이터 계층만, REST 없음 | 캐시 경유 조회는 UC-1 E2E에 포함 | REST CRUD + 캐시 무효화 (Phase 2-2) |
| [UC-5.md](UC-5.md) | 로그 아카이빙 | 미구현 | @Scheduled·OpenSearch | 없음 | — | @Scheduled export (Phase 2-4) |
| [undertow.md](undertow.md) | (전역 기반) 내장 웹서버 | 적용 완료·실측 대기 | Undertow ↔ Tomcat | `build.gradle` (Tomcat 제외 + Undertow starter) | 기동 로그 `Undertow started on port 8092` 확인 | Tomcat 대비 실측 |

> `undertow.md`는 특정 UC가 아니라 프로젝트 전역 기반이라 UC 번호 없이 둡니다. 지금 집중 연구 대상.
> 진행 상태·빌드는 [../../PROGRESS.md](../../PROGRESS.md), 사내 Kafka 라이브러리 매핑 등 프로젝트 전역 개념은 [../../NOTES.md](../../NOTES.md), 단계(Phase) 로드맵은 [../../../ROADMAP.md](../../../ROADMAP.md)에 있습니다.
