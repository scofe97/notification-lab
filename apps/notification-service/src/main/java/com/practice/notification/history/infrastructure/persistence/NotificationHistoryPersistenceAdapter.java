package com.practice.notification.history.infrastructure.persistence;

import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.history.domain.model.NotificationHistory;
import com.practice.notification.history.domain.port.out.NotificationHistoryStorePort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link NotificationHistoryStorePort}의 JPA(PostgreSQL) 구현체입니다 — outbound adapter.
 *
 * <p>일 단위 기간을 시각 범위(00:00:00 ~ 23:59:59.999...)로 변환하는 책임도 여기서 끝냅니다.
 * OpenSearch 도입 시 이 어댑터만 교체합니다.
 */
@Component
@RequiredArgsConstructor
public class NotificationHistoryPersistenceAdapter implements NotificationHistoryStorePort {

    private final NotificationHistoryJpaRepository repository;

    @Override
    public void saveAll(List<NotificationHistory> histories) {
        repository.saveAll(histories.stream()
                .map(NotificationHistoryEntity::from)
                .toList());
    }

    @Override
    public List<NotificationHistory> search(ChannelType channelType, LocalDate from, LocalDate to) {
        return repository.findByChannelTypeAndSentAtBetweenOrderBySentAtDesc(
                        channelType, from.atStartOfDay(), endOfDay(to)).stream()
                .map(NotificationHistoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<NotificationHistory> findByDay(LocalDate day) {
        return repository.findBySentAtBetween(day.atStartOfDay(), endOfDay(day)).stream()
                .map(NotificationHistoryEntity::toDomain)
                .toList();
    }

    private LocalDateTime endOfDay(LocalDate day) {
        return day.plusDays(1).atStartOfDay().minusNanos(1);
    }
}
