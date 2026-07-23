package com.practice.scenario.domain.port.out;

/**
 * 외부 발송 API 장애 주입 out-port입니다.
 *
 * <p>도메인은 "이 채널을 고장/지연/정상으로 바꿔라"만 요구하고,
 * WireMock admin API 형식은 어댑터가 압니다. 주입은 런타임 스텁이라
 * reset 또는 WireMock 재시작으로 원복됩니다.
 */
public interface FaultInjectionPort {

    /** 지정 채널의 발송 API가 500을 돌려주게 합니다. */
    void inject5xx(String channel);

    /** 지정 채널의 발송 API에 고정 지연을 주입합니다. */
    void injectDelay(String channel, long delayMs);

    /** 모든 런타임 스텁을 지우고 파일 스텁 기본값(정상 200)으로 복원합니다. */
    void reset();
}
