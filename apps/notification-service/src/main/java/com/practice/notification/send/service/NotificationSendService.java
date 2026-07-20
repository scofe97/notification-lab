package com.practice.notification.send.service;

import com.practice.notification.send.channel.ChannelSettingService;
import com.practice.notification.send.domain.ChannelType;
import com.practice.notification.send.domain.NotificationEvent;
import com.practice.notification.send.domain.SendResult;
import com.practice.notification.send.remote.NotificationSendCaller;
import com.practice.notification.send.remote.SendRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 알림 이벤트를 채널별로 분기해 발송하는 코어 서비스입니다.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>수신자를 채널 타입별로 그룹핑 (FR-2)</li>
 *   <li>채널 설정을 조회해 수신 거부한 수신자 제외 (FR-3, Caffeine 캐시)</li>
 *   <li>채널별로 외부 발송 API 호출 (FR-4, OpenFeign)</li>
 *   <li>발송 호출은 회로차단기로 감쌈 (FR-5, Resilience4j)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSendService {

    private final NotificationSendCaller sendCaller;
    private final ChannelSettingService channelSettingService;

    /**
     * 이벤트의 수신자를 채널별로 그룹핑해 각 채널로 발송하고, 채널별 결과를 돌려줍니다.
     */
    public List<SendResult> send(NotificationEvent event) {
        // ① 채널 타입별 그룹핑
        Map<ChannelType, List<NotificationEvent.Receiver>> byChannel = event.receivers().stream()
                .collect(Collectors.groupingBy(NotificationEvent.Receiver::channelType));

        return byChannel.entrySet().stream()
                .map(entry -> sendChannel(event, entry.getKey(), entry.getValue()))
                .toList();
    }

    private SendResult sendChannel(NotificationEvent event,
                                   ChannelType channelType,
                                   List<NotificationEvent.Receiver> receivers) {
        // ② 채널 설정 조회 — 수신 거부한 수신자는 목적지에서 제외
        List<String> destinations = receivers.stream()
                .filter(r -> channelSettingService.isEnabled(r.userId(), channelType))
                .map(NotificationEvent.Receiver::destination)
                .toList();

        if (destinations.isEmpty()) {
            log.debug("채널 {} 발송 대상 없음 (전부 수신거부)", channelType);
            return SendResult.of(channelType, 0, 0);
        }

        // ③④ 회로차단기로 감싼 발송 — 프록시를 거치도록 별도 빈(NotificationSendCaller)에 위임
        SendRequest request = new SendRequest(event.title(), event.content(), destinations);
        return sendCaller.callSend(channelType, request);
    }
}
