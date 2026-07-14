# notification-service — 알림 발송 관측 대상

Kafka 기반 알림 발송 흐름을 학습·관측하기 위한 **단일 Spring Boot 프로젝트**입니다. 발송과 이력 기능은 별도 애플리케이션으로 나누지 않고 이 프로젝트의 패키지로 구분합니다.

## 문서

- **설계** (요구·유스케이스·아키텍처): [docs/](docs/)
  - [docs/01-requirements.md](docs/01-requirements.md) · [docs/02-actors-usecases.md](docs/02-actors-usecases.md) · [docs/03-architecture.md](docs/03-architecture.md)
- **UC별 리뷰·연구 노트**: [docs/uc/00-index.md](docs/uc/00-index.md)
  - UC마다 한 문서로 코드 리뷰 렌즈·직접 관찰법·확인 기록을 둡니다. UC-1(발송)은 실내용, UC-2~5는 stub, `undertow.md`는 전역 기반 실측 노트입니다.
- **진행 상태·빌드·중단 지점**: [PROGRESS.md](PROGRESS.md)
- **전역 개념 학습 기록**: [NOTES.md](NOTES.md) (UC별 개념은 각 UC 문서에)

## 이 프로젝트가 담는 것

| 흐름 | 유스케이스 | 기능 요구 | 패키지 | 상태 | 개념 |
|------|-----------|-----------|--------|------|------|
| **발송** | UC-1(Kafka 발송)·UC-2(외부솔루션)·UC-4(채널설정) | FR-1~6·12 | `notification.send` | UC-1 완료, UC-2·4 이후 | Kafka 소비·Caffeine·OpenFeign·Resilience4j·DLT·Undertow |
| **이력** | UC-3(이력조회)·UC-5(아카이빙) | FR-7~11 | `notification.history` | 이후 | OpenSearch·ULID·SSL 우회(안티패턴) |

> UC-1(Kafka 발송 파이프라인)은 구현·런타임 검증까지 끝났습니다([PROGRESS.md](PROGRESS.md)). UC-2·4와 이력 흐름은 그 뒤에 추가합니다. UC별 진행·리뷰 노트는 [docs/uc/](docs/uc/00-index.md)에 있습니다.

## 패키지 구조

```
com.practice.notification
├── NotificationApplication          # 진입점 (@EnableFeignClients·@EnableCaching)
├── send/                            # 발송 흐름 (UC-1 구현 완료)
│   ├── domain/                      #   ChannelType·NotificationEvent·SendResult
│   ├── channel/                     #   채널 설정(Entity·Repository·Service[Caffeine])
│   ├── remote/                      #   NotificationSendClient(Feign)·요청/응답
│   ├── service/                     #   NotificationSendService(그룹핑·회로차단)
│   ├── listener/                    #   NotificationListener(@KafkaListener)
│   └── config/                      #   KafkaConsumerConfig(에러핸들러+DLT)·CacheConfig(Caffeine)
└── history/                         # 이력 흐름 (후속)
```

## 실행

```bash
# 1) 인프라 기동 (저장소 루트에서)
cd ~/notification-lab && docker compose -f infra/compose.yaml up -d

# 2) 앱 실행
cd ~/notification-lab/apps/notification-service && ./gradlew bootRun
```

Java 21 (toolchain으로 강제), Spring Boot 3.5.0, Undertow, 앱 포트 8092, Kafka `localhost:9192`.
