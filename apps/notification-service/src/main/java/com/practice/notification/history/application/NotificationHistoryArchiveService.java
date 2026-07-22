package com.practice.notification.history.application;

import com.practice.notification.history.domain.model.NotificationHistory;
import com.practice.notification.history.domain.port.in.ArchiveNotificationHistoryUseCase;
import com.practice.notification.history.domain.port.out.ArchiveWriterPort;
import com.practice.notification.history.domain.port.out.NotificationHistoryStorePort;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 이력 아카이빙 유스케이스 구현(application 계층)입니다(UC-5, FR-11).
 *
 * <p>대상 날짜의 이력을 저장소에서 읽어 보관 파일로 내보냅니다. 같은 날짜를 다시 실행하면
 * 같은 내용을 다시 내보낼 뿐이므로(파일 덮어쓰기) 재실행에 안전합니다 — 배치 멱등성은
 * 이 성질에 기댑니다. 저장소에서의 삭제는 보류(후속 결정)입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationHistoryArchiveService implements ArchiveNotificationHistoryUseCase {

    private final NotificationHistoryStorePort historyStorePort;
    private final ArchiveWriterPort archiveWriterPort;

    @Override
    public int archive(LocalDate day) {
        List<NotificationHistory> histories = historyStorePort.findByDay(day);
        if (histories.isEmpty()) {
            log.info("아카이빙 대상 없음: day={}", day);
            return 0;
        }

        String path = archiveWriterPort.write(day, histories);
        log.info("아카이빙 완료: day={}, {}건 → {}", day, histories.size(), path);
        return histories.size();
    }
}
