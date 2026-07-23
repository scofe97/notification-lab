package com.practice.scenario.domain.port.out;

/**
 * 알림 이벤트 발행 out-port입니다.
 *
 * <p>도메인은 "유효한/처리 불가한 이벤트를 토픽에 넣어라"만 요구하고,
 * Kafka·JSON 형식(notification-service의 NotificationEvent 호환)은 어댑터가 압니다.
 */
public interface EventPublishPort {

    /** 유효한 알림 이벤트 1건을 발행합니다. 발행한 eventId를 돌려줍니다. */
    String publishNormal(String channel);

    /** 처리 불가 메시지 1건을 발행합니다. variant(0~2)로 3변형을 고릅니다. */
    String publishPoison(int variant);
}
