package com.practice.notification.history.domain.model;

import com.practice.notification.common.domain.ChannelType;
import java.time.LocalDateTime;

/**
 * 발송 이력 한 건입니다 — 순수 도메인 모델. 수신자(목적지) 단위로 기록합니다(FR-8).
 *
 * <p>id는 시간순 정렬 가능한 ULID입니다(FR-9). 생성 시각이 ID 자체에 인코딩되어,
 * 기간 조회·아카이빙에서 정렬 키로 그대로 쓸 수 있습니다. ULID 생성은 application이 맡고
 * 이 모델은 값만 담습니다.
 */
public class NotificationHistory {

    private final String id;
    private final String eventId;
    private final ChannelType channelType;
    private final String destination;
    private final boolean succeeded;
    private final LocalDateTime sentAt;

    public NotificationHistory(String id, String eventId, ChannelType channelType,
                               String destination, boolean succeeded, LocalDateTime sentAt) {
        this.id = id;
        this.eventId = eventId;
        this.channelType = channelType;
        this.destination = destination;
        this.succeeded = succeeded;
        this.sentAt = sentAt;
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public String getDestination() {
        return destination;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
