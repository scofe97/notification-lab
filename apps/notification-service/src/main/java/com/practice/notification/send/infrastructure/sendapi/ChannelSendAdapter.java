package com.practice.notification.send.infrastructure.sendapi;

import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.send.domain.model.SendResult;
import com.practice.notification.send.domain.port.out.ChannelSendPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link ChannelSendPort}의 구현체입니다 — outbound adapter.
 *
 * <p>전송 DTO 조립과 <b>예외 → 집계 변환</b>이 이 어댑터의 책임입니다.
 * {@code NotificationSendCaller}(회로차단기 프록시)는 실패 시 예외를 던지고 — 그래야
 * 회로차단기가 실패를 기록합니다 — 이 어댑터가 그 예외를 잡아 {@code SendResult.failure}로
 * 변환합니다. 회로차단기 동작과 "포트는 집계를 반환한다"는 계약이 양립하는 지점입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelSendAdapter implements ChannelSendPort {

    private final NotificationSendCaller sendCaller;

    @Override
    public SendResult send(ChannelType channelType, String title, String content, List<String> destinations) {
        SendRequest request = new SendRequest(title, content, destinations);
        try {
            return sendCaller.callSend(channelType, request);
        } catch (Exception e) {
            log.warn("채널 {} 발송 실패 ({}건): {}", channelType, destinations.size(), e.toString());
            return SendResult.failure(channelType, destinations.size());
        }
    }
}
