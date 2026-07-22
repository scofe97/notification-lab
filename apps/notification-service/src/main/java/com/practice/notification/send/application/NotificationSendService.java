package com.practice.notification.send.application;

import com.practice.notification.channel.domain.port.in.GetChannelSettingUseCase;
import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.send.domain.model.NotificationEvent;
import com.practice.notification.send.domain.model.SendResult;
import com.practice.notification.send.domain.port.in.SendNotificationUseCase;
import com.practice.notification.send.domain.port.out.ChannelSendPort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 알림 발송 유스케이스 구현(application 계층)입니다.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>수신자를 채널 타입별로 그룹핑 (FR-2)</li>
 *   <li>채널 설정을 조회해 수신 거부한 수신자 제외 (FR-3 — channel 컨텍스트의 in-port 경유, Caffeine 캐시)</li>
 *   <li>채널별 발송을 out-port에 위임 (FR-4·5 — Feign·회로차단기는 어댑터 뒤)</li>
 * </ol>
 *
 * <p>실패는 예외가 아니라 {@link SendResult}의 실패 집계로 돌아옵니다({@link ChannelSendPort} 계약).
 * 이 계층은 실패에 의미를 부여하지 않고 사실만 모아 돌려줍니다 — 재시도(예외)로 해석할지
 * 응답 코드(207)로 해석할지는 입구 어댑터의 몫입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSendService implements SendNotificationUseCase {

    private final ChannelSendPort channelSendPort;
    private final GetChannelSettingUseCase channelSettingUseCase;

    @Override
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
                .filter(r -> channelSettingUseCase.isEnabled(r.userId(), channelType))
                .map(NotificationEvent.Receiver::destination)
                .toList();

        if (destinations.isEmpty()) {
            log.debug("채널 {} 발송 대상 없음 (전부 수신거부)", channelType);
            return SendResult.of(channelType, 0, 0);
        }

        // ③ 발송은 out-port에 위임 — 실패도 집계로 돌아온다
        return channelSendPort.send(channelType, event.title(), event.content(), destinations);
    }
}
