package com.practice.scenario.domain.model;

/**
 * 시나리오 실행 요청입니다 — 순수 도메인 모델.
 *
 * @param type       시나리오 종류
 * @param count      발행 건수 (발행 계열만 사용)
 * @param intervalMs 발행 간격 밀리초 (NORMAL만 사용 — BURST는 0으로 강제)
 * @param channel    대상 채널 소문자 표기: sms·email·alimtalk (발행 수신 채널이자 장애 주입 대상)
 * @param delayMs    지연 밀리초 (FAULT_DELAY만 사용)
 */
public record ScenarioRequest(
        ScenarioType type,
        int count,
        long intervalMs,
        String channel,
        long delayMs
) {
}
