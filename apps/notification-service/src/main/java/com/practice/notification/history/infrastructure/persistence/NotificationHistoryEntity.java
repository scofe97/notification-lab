package com.practice.notification.history.infrastructure.persistence;

import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.history.domain.model.NotificationHistory;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발송 이력 JPA 엔티티입니다 — outbound adapter의 영속 모델.
 *
 * <p>PK는 ULID 문자열입니다(FR-9). 조회(UC-3)와 아카이빙(UC-5)이 채널·발송시각으로 거르므로
 * (channel_type, sent_at) 복합 인덱스를 둡니다.
 */
@Entity
@Table(name = "notification_history",
        indexes = @Index(name = "idx_history_channel_sent", columnList = "channelType, sentAt"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryEntity {

    @Id
    private String id;

    private String eventId;

    @Enumerated(EnumType.STRING)
    private ChannelType channelType;

    private String destination;

    private boolean succeeded;

    private LocalDateTime sentAt;

    static NotificationHistoryEntity from(NotificationHistory history) {
        return new NotificationHistoryEntity(history.getId(), history.getEventId(),
                history.getChannelType(), history.getDestination(),
                history.isSucceeded(), history.getSentAt());
    }

    NotificationHistory toDomain() {
        return new NotificationHistory(id, eventId, channelType, destination, succeeded, sentAt);
    }
}
