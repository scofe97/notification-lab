package com.practice.notification.dispatch.infrastructure;

import com.practice.notification.dispatch.domain.model.ChannelDispatchResult;
import com.practice.notification.dispatch.domain.model.Recipient;
import com.practice.notification.dispatch.domain.port.out.NotificationSendPort;
import com.practice.notification.send.domain.NotificationEvent;
import com.practice.notification.send.domain.SendResult;
import com.practice.notification.send.service.NotificationSendService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link NotificationSendPort}의 구현체입니다 — send 컨텍스트 완충 어댑터.
 *
 * <p>dispatch의 수신자 목록을 send의 {@link NotificationEvent}로 조립해 기존 발송 서비스에
 * 위임합니다. 채널 그룹핑·설정 캐시 필터·회로차단기는 전부 그쪽 경로를 재사용합니다.
 * send가 in-port 없는 레거시 구조라 구체 클래스를 여기서만 알고, 도메인에는 새지 않게 합니다.
 */
@Component
@RequiredArgsConstructor
public class NotificationSendAdapter implements NotificationSendPort {

    private static final String EVENT_ID_PREFIX = "rest-";

    private final NotificationSendService notificationSendService;

    @Override
    public List<ChannelDispatchResult> send(String title, String content, List<Recipient> recipients) {
        List<NotificationEvent.Receiver> receivers = recipients.stream()
                .map(r -> new NotificationEvent.Receiver(r.getUserId(), r.getChannelType(), r.getDestination()))
                .toList();
        NotificationEvent event = new NotificationEvent(
                EVENT_ID_PREFIX + UUID.randomUUID(), title, content, receivers);

        return notificationSendService.send(event).stream()
                .map(this::toDomain)
                .toList();
    }

    private ChannelDispatchResult toDomain(SendResult result) {
        return new ChannelDispatchResult(
                result.channelType(), result.requested(), result.succeeded(), result.failed());
    }
}
