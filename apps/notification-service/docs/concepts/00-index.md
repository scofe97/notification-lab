# 개념 노트 — 인덱스

이 폴더는 특정 UC에 매이지 않는 **기술 개념**을 모읍니다. UC 문서가 "이 기능이 어떻게 동작하는가"를 다룬다면, 여기서는 "왜 그렇게 동작하는가"를 다룹니다. UC-2 이후에도 같은 개념을 다시 설명하지 않고 이 문서를 링크합니다.

문서의 사실은 UC-1 구현과 2026-07-20 실측 실습에서 확인한 것을 근거로 씁니다. 실행으로 보지 않은 것은 그렇다고 밝힙니다.

| 문서 | 다루는 것 |
|------|-----------|
| [Kafka 메시지 처리](kafka-message-handling.md) | 오프셋과 커밋, ack-mode, 재시도 단위, DLT 적재와 재처리, 캐시가 consumer lag에 미치는 영향 |
| [외부 호출과 회복 탄력성](external-call-and-resilience.md) | WireMock 스텁과 장애 연출, Feign의 예외 번역, CircuitBreaker 상태 전이와 프록시 AOP |

## 관련 문서

- 구조와 인프라 구성: [03-architecture.md](../03-architecture.md)
- UC별 리뷰 렌즈와 관찰법: [uc/00-index.md](../uc/00-index.md)
- 이해 루프 기록(실측 근거): [learning/00-index.md](../learning/00-index.md)
