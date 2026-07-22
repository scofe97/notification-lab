package com.practice.notification.dispatch.infrastructure.send;

import com.practice.notification.dispatch.domain.model.ChannelDispatchResult;
import com.practice.notification.dispatch.domain.model.Recipient;
import com.practice.notification.dispatch.domain.port.out.NotificationSendPort;
import com.practice.notification.send.domain.model.NotificationEvent;
import com.practice.notification.send.domain.model.SendResult;
import com.practice.notification.send.domain.port.in.SendNotificationUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link NotificationSendPort}의 구현체입니다 — send 컨텍스트 완충 어댑터.
 *
 * <p>dispatch의 수신자 목록을 send의 {@link NotificationEvent}로 조립해 send의
 * in-port({@link SendNotificationUseCase})에 위임합니다. 채널 그룹핑·설정 캐시 필터·회로차단기는
 * 전부 그쪽 경로를 재사용하고, 결과는 dispatch의 언어({@code ChannelDispatchResult})로 역번역합니다.
 * 실패는 집계로 돌아오므로(2026-07-22 실패 계약) 예외 관통 없이 207/502 분기가 동작합니다.
 */
@Component
@RequiredArgsConstructor
public class NotificationSendAdapter implements NotificationSendPort {

    private static final String EVENT_ID_PREFIX = "rest-";

    private final SendNotificationUseCase sendNotificationUseCase;

    @Override
    public List<ChannelDispatchResult> send(String title, String content, List<Recipient> recipients) {
        List<NotificationEvent.Receiver> receivers = recipients.stream()
                .map(r -> new NotificationEvent.Receiver(r.getUserId(), r.getChannelType(), r.getDestination()))
                .toList();
        NotificationEvent event = new NotificationEvent(
                EVENT_ID_PREFIX + UUID.randomUUID(), title, content, receivers);

        return sendNotificationUseCase.send(event).stream()
                .map(this::toDomain)
                .toList();
    }

    private ChannelDispatchResult toDomain(SendResult result) {
        return new ChannelDispatchResult(
                result.channelType(), result.requested(), result.succeeded(), result.failed());
    }
}
