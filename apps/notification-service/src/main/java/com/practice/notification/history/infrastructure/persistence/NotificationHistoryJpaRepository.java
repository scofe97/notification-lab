package com.practice.notification.history.infrastructure.persistence;

import com.practice.notification.common.domain.ChannelType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 발송 이력 Spring Data 저장소입니다 — 어댑터 내부 구현.
 */
public interface NotificationHistoryJpaRepository
        extends JpaRepository<NotificationHistoryEntity, String> {

    List<NotificationHistoryEntity> findByChannelTypeAndSentAtBetweenOrderBySentAtDesc(
            ChannelType channelType, LocalDateTime from, LocalDateTime to);

    List<NotificationHistoryEntity> findBySentAtBetween(LocalDateTime from, LocalDateTime to);
}
