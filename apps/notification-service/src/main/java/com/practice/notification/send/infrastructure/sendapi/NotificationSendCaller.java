package com.practice.notification.send.infrastructure.sendapi;

import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.send.domain.model.SendResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 발송 API 호출을 회로차단기로 감싸는 어댑터입니다(FR-5).
 *
 * <p><b>왜 별도 빈인가</b>: {@code @CircuitBreaker}는 Spring 프록시 AOP로 동작하므로,
 * 같은 객체 안에서 자기 메서드를 직접 부르면(self-invocation) 프록시를 우회해 어노테이션이
 * 적용되지 않습니다. 호출이 반드시 프록시를 거치도록 발송 호출을 별도 빈으로 분리했습니다.
 * (2026-07-20 실측: 분리 전에는 24건 연속 실패에도 회로가 열리지 않았음 — 학습 문서 Phase 4 기록)
 *
 * <p>fallback은 지정하지 않고 예외를 그대로 던집니다 — 그래야 회로차단기가 실패를 기록합니다. 예외는 바로 바깥의 ChannelSendAdapter가 잡아 실패 집계로 변환합니다(2026-07-22 실패 계약).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSendCaller {

    private final NotificationSendClient sendClient;

    /**
     * 외부 발송 API를 호출합니다. 반복 실패 시 회로가 열려 이후 호출을 즉시 차단합니다(FR-5).
     */
    @CircuitBreaker(name = "notificationSend")
    public SendResult callSend(ChannelType channelType, SendRequest request) {
        String channelPath = channelType.name().toLowerCase();
        sendClient.send(channelPath, request);

        int count = request.destinations().size();
        log.debug("채널 {} 발송 성공: {}건", channelType, count);

        return SendResult.of(channelType, count, count);
    }
}
