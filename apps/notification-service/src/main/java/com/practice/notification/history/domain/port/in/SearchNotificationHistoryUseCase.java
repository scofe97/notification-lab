package com.practice.notification.history.domain.port.in;

import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.history.domain.model.NotificationHistory;
import java.time.LocalDate;
import java.util.List;

/**
 * 발송 이력 조회 유스케이스(in-port)입니다(UC-3, FR-10).
 *
 * <p>채널·기간(일 단위)으로 검색합니다. REST 어댑터가 진입합니다.
 */
public interface SearchNotificationHistoryUseCase {

    /** 채널과 기간(from~to, 양끝 포함)으로 이력을 조회합니다. 최신순으로 돌려줍니다. */
    List<NotificationHistory> search(ChannelType channelType, LocalDate from, LocalDate to);
}
