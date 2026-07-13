# a-notification — 학습 노트 (전역 개념)

이 문서는 UC에 종속되지 않는 **프로젝트 전역 개념**만 둡니다. UC별 리뷰·관찰·확인 기록은 [docs/uc/](docs/uc/00-index.md)로 분리했습니다.

- **UC별 노트**: [docs/uc/00-index.md](docs/uc/00-index.md) — UC-1~5 각각의 코드 리뷰 렌즈·실측법·확인 기록
- **진행 상태·빌드**: [PROGRESS.md](PROGRESS.md)
- **단계(Phase) 로드맵·LGMT 스터디 계획**: [../ROADMAP.md](../ROADMAP.md)
- **설계(요구·액터·아키텍처)**: [docs/](docs/) 01~03

## 개념 역링크 (원본 분석 문서)

원본 분석 문서 루트: Drive `비공개 분석 노트/`

- 전체 흐름: `원본 커스텀/01-backend.md` §5.1
- Kafka 소비: `원본 커스텀/concepts/kafka.md`
- Undertow: `원본 커스텀/concepts/undertow.md`
- Caffeine: `사내 CMP 플랫폼/concepts/caffeine-cache.md`
- OpenFeign: `사내 CMP 플랫폼/concepts/open-feign.md`
- Resilience4j: `사내 CMP 플랫폼/concepts/resilience4j-circuit-breaker.md`
- 사내 Kafka 라이브러리 구조: (비공개 메모)

> 위 경로는 비공개 분석 노트 내부의 상대경로라 이 저장소에서는 열리지 않습니다.
> UC별 개념 역링크는 각 UC 문서 §1에 그 UC가 쓰는 기술만 추려 다시 둡니다.

## 사내 Kafka 라이브러리 → Spring Kafka 대체 매핑 (핵심 학습)

원본은 사내 Kafka 라이브러리를 씁니다. 그 추상화를 표준 Spring Kafka로 풀어 본 매핑입니다. (UC-1의 핵심 학습이기도 해 [docs/uc/UC-1.md](docs/uc/UC-1.md) §1에도 둡니다.)

| 사내 Kafka 라이브러리 (원본) | Spring Kafka (이 프로젝트) |
|--------------------|----------------------------|
| `@TopicHandler` | `@KafkaListener(topics=...)` |
| `MessageHandler<T>` | 리스너 메서드 시그니처 `void handle(T msg)` |
| `FailurePolicy.DLQ` | `DeadLetterPublishingRecoverer` + `DefaultErrorHandler` |
| `FailurePolicy.LOG` | 에러 핸들러에서 로깅 후 skip |
| `TypeMappedMessageProducer` | `KafkaTemplate<K,V>` |
