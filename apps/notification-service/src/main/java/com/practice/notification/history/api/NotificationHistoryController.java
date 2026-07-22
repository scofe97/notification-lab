package com.practice.notification.history.api;

import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.history.domain.port.in.ArchiveNotificationHistoryUseCase;
import com.practice.notification.history.domain.port.in.SearchNotificationHistoryUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 발송 이력 REST 계약입니다(UC-3, FR-10) — inbound adapter.
 *
 * <p>조회는 채널·기간(일 단위, 양끝 포함)으로 검색합니다. 아카이브 수동 재실행은
 * 스케줄 실패 시 운영자가 특정 날짜를 다시 내보내는 용도입니다(UC-5의 운영 보조 계약).
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class NotificationHistoryController {

    private final SearchNotificationHistoryUseCase searchNotificationHistoryUseCase;
    private final ArchiveNotificationHistoryUseCase archiveNotificationHistoryUseCase;

    /** 채널·기간으로 이력을 조회합니다. 예: GET /api/history?channelType=SMS&from=2026-07-22&to=2026-07-22 */
    @GetMapping
    public List<HistoryResponse> search(
            @RequestParam ChannelType channelType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return searchNotificationHistoryUseCase.search(channelType, from, to).stream()
                .map(HistoryResponse::from)
                .toList();
    }

    /** 특정 날짜의 아카이빙을 수동 재실행합니다. 예: POST /api/history/archive/2026-07-22 */
    @PostMapping("/archive/{day}")
    public Map<String, Object> archive(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        int archived = archiveNotificationHistoryUseCase.archive(day);

        return Map.of("day", day.toString(), "archived", archived);
    }
}
