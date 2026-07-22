package com.practice.notification.history.domain.port.out;

import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.history.domain.model.NotificationHistory;
import java.time.LocalDate;
import java.util.List;

/**
 * 발송 이력 저장소 out-port입니다.
 *
 * <p>도메인이 저장·검색 요구를 선언하고 infrastructure가 구현합니다. 현재 구현은
 * PostgreSQL(JPA)이며, OpenSearch 도입 시 이 포트의 어댑터만 교체합니다(2026-07-22 결정 —
 * 검색엔진은 후속 후보로 보류).
 */
public interface NotificationHistoryStorePort {

    void saveAll(List<NotificationHistory> histories);

    /** 채널·기간(양끝 포함) 검색, 최신순. */
    List<NotificationHistory> search(ChannelType channelType, LocalDate from, LocalDate to);

    /** 특정 날짜의 전체 이력 (아카이빙 대상 조회). */
    List<NotificationHistory> findByDay(LocalDate day);
}
