package com.practice.notification.history.domain.port.out;

import com.practice.notification.history.domain.model.NotificationHistory;
import java.time.LocalDate;
import java.util.List;

/**
 * 아카이브 기록 out-port입니다(UC-5).
 *
 * <p>도메인은 "이 날짜의 이력을 보관 형태로 내보내라"만 요구하고, 파일 형식(NDJSON)·경로는
 * infrastructure가 정합니다.
 */
public interface ArchiveWriterPort {

    /** 이력을 보관 파일로 씁니다. 생성(또는 덮어쓴) 파일 경로 문자열을 돌려줍니다. */
    String write(LocalDate day, List<NotificationHistory> histories);
}
