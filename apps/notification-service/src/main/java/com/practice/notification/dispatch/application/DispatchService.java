package com.practice.notification.dispatch.application;

import com.practice.notification.dispatch.domain.model.ChannelDispatchResult;
import com.practice.notification.dispatch.domain.model.Recipient;
import com.practice.notification.dispatch.domain.port.in.DispatchNotificationUseCase;
import com.practice.notification.dispatch.domain.port.out.NotificationSendPort;
import com.practice.notification.dispatch.domain.port.out.RecipientLookupPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 외부 REST 발송 유스케이스 구현(application 계층)입니다(UC-2, FR-6).
 *
 * <p>흐름은 두 out-port의 조합입니다: 수신자 조회 → 발송 위임.
 * UC-1과의 차이는 트리거(REST vs Kafka)와 수신자 확정 단계(조회 API)가 하나 더 있다는 점이며,
 * 조회 이후의 발송(채널 그룹핑·설정 필터·회로차단기)은 같은 경로를 재사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService implements DispatchNotificationUseCase {

    private final RecipientLookupPort recipientLookupPort;
    private final NotificationSendPort notificationSendPort;

    @Override
    public List<ChannelDispatchResult> dispatch(String groupId, String title, String content) {
        List<Recipient> recipients = recipientLookupPort.findByGroup(groupId);
        if (recipients.isEmpty()) {
            log.debug("그룹 {} 수신자 없음 — 발송 생략", groupId);
            return List.of();
        }

        log.debug("그룹 {} 수신자 {}명 발송 시작", groupId, recipients.size());
        return notificationSendPort.send(title, content, recipients);
    }
}
