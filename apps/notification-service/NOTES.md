# notification-service — 학습 노트

이 문서는 UC에 종속되지 않는 전역 개념을 기록합니다. UC별 코드 리뷰와 실측 절차는 [UC 인덱스](docs/uc/00-index.md)에 있습니다.

- [UC별 노트](docs/uc/00-index.md)
- [진행 상태·빌드](PROGRESS.md)
- [전체 로드맵](../../ROADMAP.md)
- [관측 스터디 계획](../../observability/docs/00-study-plan.md)
- [설계 문서](docs/)

## 표준 Spring Kafka 매핑

메시지 처리 프레임워크가 감추는 역할을 표준 Spring Kafka 구성요소로 확인합니다.

| 관심사 | Spring Kafka 구현 |
|--------|------------------|
| 토픽 구독 | `@KafkaListener(topics=...)` |
| 메시지 처리 | 리스너 메서드 |
| 실패 격리 | `DeadLetterPublishingRecoverer` + `DefaultErrorHandler` |
| 실패 기록 후 건너뛰기 | 에러 핸들러 정책 |
| 메시지 발행 | `KafkaTemplate<K, V>` |
