package com.practice.scenario.domain.model;

/**
 * 실행할 시나리오의 종류입니다 — 순수 도메인 모델.
 *
 * <p>발행 계열(NORMAL·BURST·POISON)은 Kafka로 신호를 만들고,
 * 장애 계열(FAULT_*)은 WireMock의 응답 조건을 바꿉니다.
 */
public enum ScenarioType {

    /** 일정 간격의 유효 이벤트 — 정상 기준선 (관측 UC-01) */
    NORMAL,
    /** 간격 없는 연속 발행 — consumer lag 실험 (관측 UC-06) */
    BURST,
    /** 처리 불가 메시지 3변형 순환 — DLT 실험 (관측 UC-02) */
    POISON,
    /** 지정 채널 발송 API를 500으로 — CB·DLT 실험 (관측 UC-03) */
    FAULT_5XX,
    /** 지정 채널 발송 API에 지연 주입 — 외부 지연 실험 (관측 UC-04) */
    FAULT_DELAY,
    /** WireMock을 파일 스텁 기본값으로 복원 — 실험 종료 절차 */
    FAULT_RESET;

    /** CLI 표기(`fault-5xx` 등)를 타입으로 변환합니다. */
    public static ScenarioType from(String value) {
        return valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
