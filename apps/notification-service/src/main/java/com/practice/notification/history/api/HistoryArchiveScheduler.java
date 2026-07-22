package com.practice.notification.history.api;

import com.practice.notification.history.domain.port.in.ArchiveNotificationHistoryUseCase;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 전일 이력 아카이빙 스케줄러입니다(UC-5, FR-11) — inbound adapter.
 *
 * <p>이 UC의 액터는 사람이 아니라 <b>시간</b>입니다. cron이 트리거이고, 어댑터는 "전일"이라는
 * 날짜 결정만 하고 in-port를 호출합니다. 실패는 로그로 남기고 다음 주기를 기다립니다 —
 * 놓친 날짜는 수동 재실행 API(POST /api/history/archive/{day})로 보완합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryArchiveScheduler {

    private final ArchiveNotificationHistoryUseCase archiveNotificationHistoryUseCase;

    @Scheduled(cron = "${notification.history.archive-cron}")
    public void archiveYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            archiveNotificationHistoryUseCase.archive(yesterday);
        } catch (Exception e) {
            log.error("전일 아카이빙 실패: day={} — 수동 재실행 API로 보완 가능", yesterday, e);
        }
    }
}
