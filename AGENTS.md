# 코드 컨벤션 — notification-lab

이 저장소에서 **새로 작성하거나 구조를 바꾸는 모든 코드**는 아래 헥사고날 컨벤션을 따른다 (2026-07-21 합의).

## 헥사고날 패키지 구조

컨텍스트(도메인 경계) 단위로 최상위 패키지를 두고, 내부를 4계층으로 나눈다. 첫 적용 사례는 `com.practice.notification.channel`이다.

```text
<context>/
├── api/              # inbound adapter — REST Controller·Kafka Consumer·요청/응답 DTO. in-port만 호출
├── application/      # 유스케이스 구현 — in-port 구현체. 도메인 조합 + out-port 호출.
│                     #   @Service·@Transactional·@Cacheable 등 Spring 어노테이션 허용
├── domain/
│   ├── model/        # 순수 POJO — JPA·Spring·Lombok 등 프레임워크 어노테이션 금지
│   └── port/
│       ├── in/       # 유스케이스 인터페이스. 외부(api·타 컨텍스트)가 이걸로 진입
│       └── out/      # 저장소·외부시스템 인터페이스. 도메인이 선언, infrastructure가 구현
└── infrastructure/   # outbound adapter — 대상 시스템별 하위 패키지로 나눈다 (평면 배치 금지)
    ├── persistence/  #   JPA Entity·Spring Data Repository·영속 Port 구현체
    └── <외부시스템>/  #   외부 API별 하위 패키지 (예: recipient/, send/) —
                      #   Feign 클라이언트·전송 DTO·Port 구현체를 같은 곳에
```

infrastructure 하위 패키지 규칙 (2026-07-22): 한 어댑터를 이루는 클라이언트·전송 모델·Port 구현체는 **대상 시스템 단위의 같은 하위 패키지**에 둔다. "이 폴더 = 이 외부 시스템과의 연결 전부"가 되게 하고, 엔티티↔도메인 변환은 그 하위 패키지 안에서 끝낸다.

## 의존 규칙

- 의존 방향: `api → application → domain ← infrastructure` (infrastructure가 domain의 out-port를 구현하는 역의존).
- domain은 어떤 프레임워크도 import하지 않는다. 다른 컨텍스트 진입은 반드시 그쪽 in-port로만 한다 — 예: 발송 파이프라인은 `GetChannelSettingUseCase`를 주입받고, `ChannelSettingService` 구체 클래스를 모른다.
- JPA 엔티티는 infrastructure 전용이다. 클래스명이 테이블명을 암묵 결정하지 않도록 `@Table(name = ...)`을 명시한다.
- 공유 커널: 세 컨텍스트 이상이 쓰는 순수 타입은 `common/domain/`으로 승격한다. `ChannelType`이 첫 사례다(2026-07-22 승격 — send·channel·dispatch 공유). `common`에는 프레임워크 import 없는 순수 타입만 둔다.
- 입구별 실패 의미론: 발송 같은 공용 유스케이스의 실패는 in-port에서 **집계로 반환**하고, 예외(재시도·DLT)로 해석할지 응답 코드(207/502)로 해석할지는 **입구 어댑터가 번역**한다 (2026-07-22 — UC-2 반증 실측의 교훈).

## 적용 범위

- **전 컨텍스트 적용 완료** (2026-07-22) — send·channel·dispatch 모두 이 구조를 따른다. 신규 코드·신규 컨텍스트도 즉시 적용.

## 원본 컨벤션 출처

redpanda-playground hexagonal-guide의 4계층 구조를 채택했다 (api / application / domain(port 선언) / infrastructure, 순수 도메인 + 역의존).

## 공개 저장소 규칙

이 저장소는 public이다. 커밋 전에 회사명·인명·조직 고유 정보가 없는지 확인한다 (`grep -ri` 게이트).
