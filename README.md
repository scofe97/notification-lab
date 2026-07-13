# notification-lab — 원본·사내 CMP 플랫폼 개념 체득 미니 프로젝트

새 팀 이관 후 분석한 원본(사내 알림 서비스)·사내 CMP 플랫폼 저장소의 개념을 **직접 손으로 구현해 체득**하는 실습 저장소입니다. 목적은 두 가지입니다. 개념을 코드로 만들어 확실히 이해하고, 그렇게 이해한 바탕으로 **팀원들이 기존에 해온 업무를 읽어내는 것**입니다. 실제 클라우드(AWS·NCP·vSphere)나 사내 인프라(사내 Kafka 라이브러리, 사내 Schema Registry)는 쓰지 않고, **전부 오픈소스 + Docker로 대체**합니다.

> 분석 개념 원본은 Drive의 `비공개 분석 노트/` 아래에 있고, 이 저장소는 그 개념을 재현하는 코드와 학습 노트를 담습니다. 코드는 Drive 밖(`~/notification-lab/`)에 두어 git·빌드·Docker가 정상 동작하게 합니다.

## 두 축

팀의 두 저장소에 각각 대응하는 두 축을 나란히 세웁니다. 두 축은 소프트웨어 성격도 대비됩니다. 원본은 이벤트를 소비해 발송하는 파이프라인이고, 사내 CMP 플랫폼은 자원을 조회·관리하는 플랫폼입니다.

| 축 | 대응 저장소 | 성격 | 상태 |
|----|------------|------|------|
| **A. 원본 알림 서비스** | 사내 알림 서비스 | 이벤트 소비 → 분기 발송 | UC-1 발송 파이프라인 구현·검증 완료. 이력(UC-3·5)은 이후 |
| **B. 사내 CMP 플랫폼 + inventory** | 사내 CMP 플랫폼 | 자원 조회·관리 플랫폼 | 이후 진행 |

> 이번 단계는 **축 A(원본) 백엔드 위주**입니다. 프론트엔드는 최소화하고(요청 진입점 정도만), 백엔드 개념 재현에 집중합니다.
> 단계(Phase)별 진행 위치와 다음 국면(**LGMT Observability 스터디**)은 [ROADMAP.md](ROADMAP.md)가 SSOT입니다.

## 프로젝트 구성

```
~/notification-lab/
  README.md                         # 이 파일 — 전체 지도
  ROADMAP.md                        # 단계(Phase) 로드맵 + LGMT 스터디 계획 — 진행단계 SSOT
  infra/                            # 도커 인프라 전체 (compose + 서비스별 설정)
    docker-compose.yml              #   핵심 스택 — 지금 사용 (Kafka·kafka-ui·Redis·OpenSearch·MailHog·WireMock)
    docker-compose.lgmt.yml         #   Phase 3 관측 스택 스켈레톤 (Loki·Mimir·Tempo·Grafana·Alloy·MariaDB)
    wiremock/                       #   WireMock 발송 목 스텁
    loki/ · mimir/ · tempo/ · alloy/ · grafana/ · mariadb/   # Phase 3 설정 placeholder (TODO)

  # ── 축 A: 원본 알림 서비스 (단일 프로젝트) ──
  a-notification/                   # 발송(send)·이력(history) 흐름을 한 프로젝트에서
    README.md · PROGRESS.md · NOTES.md
    docs/                           #   설계: 01-requirements · 02-actors-usecases · 03-architecture
    docs/uc/                        #   UC별 리뷰·연구 노트: UC-1~5 · UC-1-dlt · undertow (00-index)
    src/.../notification/send/      #   발송 구현 (listener·config·service·channel·remote·domain)

  # ── 축 B: 사내 CMP 플랫폼 + inventory (이후) ──
  # b-platform/ 등 — 착수 시 확정
```

## 개념 → 프로젝트 매핑 (축 A)

재현 프로젝트는 단일 `a-notification`이고, 흐름을 `send`(발송)·`history`(이력) 패키지로 나눕니다. 아래 "패키지" 열이 그 구분입니다.

| 개념 | 원본 서비스 | 패키지 | 상태 | 오픈소스 대체 |
|------|-------------|--------|------|----------------|
| Kafka 이벤트 소비 | 사내 Kafka 라이브러리 `@TopicHandler` | send | ✅ | Spring Kafka `@KafkaListener` |
| 발송 실패 격리 | 사내 Kafka 라이브러리 `FailurePolicy.DLQ` | send | ✅ | `DeadLetterPublishingRecoverer` |
| 채널 설정 캐시 | (사내 CMP 플랫폼 Caffeine 개념 차용) | send | ✅ | Caffeine |
| 선언적 HTTP·회로차단 | OpenFeign + (Resilience4j) | send | ✅ | OpenFeign + Resilience4j |
| 내장 웹서버 교체 | Undertow | (전역) | ✅ | Undertow |
| 외부 발송(SMS·메일) | NCP SENS | send·history | 부분(SMS 목) | WireMock(SMS 목) + MailHog(메일) |
| 로그 아카이빙 | OpenSearch 배치 | history | 이후 | OpenSearch (Docker) |
| 이력 ID | (사내 CMP 플랫폼 ULID 개념 차용) | history | 이후 | ULID |
| SSL 검증 우회 | `SslConfig` | history | 이후 | 재현 + 위험성 주석 |

> ✅ = 구현·검증 완료 (UC-1). 상세는 `a-notification/docs/uc/`의 UC별 노트 참조.

> Go(asynq·uber-fx·go-workspace)·프론트(Nx·Module Federation·Vite) 개념은 이번 백엔드 범위 밖입니다. 별도 랩 후보로만 남겨둡니다.

## 스택 (Drive 분석 문서 근거)

- Java 21 고정 (Java 25 금지)
- Spring Boot 3.5.x / Undertow / Spring Kafka / OpenFeign / Resilience4j / Caffeine / OpenSearch client
- H2 파일모드 (원본 로컬도 H2), 인프라는 Docker Compose

축 A 상세는 `a-notification/`을 참조하세요. 설계는 `docs/`(01-requirements·02-actors-usecases·03-architecture), UC별 리뷰·관찰 노트는 `docs/uc/`(00-index부터), 진행 상태는 `PROGRESS.md`, 전역 개념 역링크는 `NOTES.md`에 있습니다.
