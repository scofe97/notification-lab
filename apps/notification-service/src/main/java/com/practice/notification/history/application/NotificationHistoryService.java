package com.practice.notification.history.application;

import com.github.f4b6a3.ulid.UlidCreator;
import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.history.domain.model.NotificationHistory;
import com.practice.notification.history.domain.port.in.RecordNotificationHistoryUseCase;
import com.practice.notification.history.domain.port.in.SearchNotificationHistoryUseCase;
import com.practice.notification.history.domain.port.out.NotificationHistoryStorePort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 이력 기록·조회 유스케이스 구현(application 계층)입니다(UC-3, FR-8·9·10).
 *
 * <p><b>기록은 best-effort</b>: 저장 실패를 호출자에게 전파하지 않습니다. 이력 기록 실패가
 * 발송 재시도를 유발하면 이미 성공한 발송이 중복되기 때문입니다(2026-07-22 실측 교훈).
 * 유실된 이력은 경고 로그로 보완 추적합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationHistoryService
        implements RecordNotificationHistoryUseCase, SearchNotificationHistoryUseCase {

    private final NotificationHistoryStorePort historyStorePort;

    @Override
    public void record(String eventId, ChannelType channelType, List<String> destinations, boolean succeeded) {
        try {
            LocalDateTime sentAt = LocalDateTime.now();
            List<NotificationHistory> histories = destinations.stream()
                    .map(destination -> new NotificationHistory(
                            UlidCreator.getUlid().toString(), eventId, channelType,
                            destination, succeeded, sentAt))
                    .toList();
            historyStorePort.saveAll(histories);
        } catch (Exception e) {
            log.warn("이력 기록 실패 (발송에는 영향 없음): eventId={}, channel={}, {}건 — {}",
                    eventId, channelType, destinations.size(), e.toString());
        }
    }

    @Override
    public List<NotificationHistory> search(ChannelType channelType, LocalDate from, LocalDate to) {
        return historyStorePort.search(channelType, from, to);
    }
}
