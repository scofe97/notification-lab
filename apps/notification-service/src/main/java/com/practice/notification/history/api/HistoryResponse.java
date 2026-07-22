package com.practice.notification.history.api;

import com.practice.notification.history.domain.model.NotificationHistory;
import java.time.LocalDateTime;

/**
 * 이력 조회 응답 한 건입니다(FR-10).
 *
 * @param id          이력 ID (ULID — 시간순 정렬 가능)
 * @param eventId     발송을 일으킨 이벤트 ID
 * @param channelType 채널
 * @param destination 목적지 (전화번호/이메일)
 * @param succeeded   발송 성공 여부
 * @param sentAt      발송 시각
 */
public record HistoryResponse(
        String id,
        String eventId,
        String channelType,
        String destination,
        boolean succeeded,
        LocalDateTime sentAt
) {

    static HistoryResponse from(NotificationHistory history) {
        return new HistoryResponse(history.getId(), history.getEventId(),
                history.getChannelType().name(), history.getDestination(),
                history.isSucceeded(), history.getSentAt());
    }
}
