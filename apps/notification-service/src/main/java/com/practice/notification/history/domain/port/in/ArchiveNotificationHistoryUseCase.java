package com.practice.notification.history.domain.port.in;

import java.time.LocalDate;

/**
 * 발송 이력 아카이빙 유스케이스(in-port)입니다(UC-5, FR-11).
 *
 * <p>스케줄러(시간 트리거) 또는 수동 재실행 API가 진입합니다. 대상 날짜의 이력을
 * 파일로 내보내며, 저장소에서 지우지는 않습니다(삭제는 보류 — 후속 결정).
 */
public interface ArchiveNotificationHistoryUseCase {

    /** 해당 날짜의 이력을 파일로 내보내고, 내보낸 건수를 돌려줍니다. */
    int archive(LocalDate day);
}
