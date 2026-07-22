package com.practice.notification.dispatch.api;

import com.practice.notification.dispatch.domain.model.ChannelDispatchResult;
import com.practice.notification.dispatch.domain.model.DispatchStatus;
import com.practice.notification.dispatch.domain.port.in.DispatchNotificationUseCase;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 외부 시스템의 REST 발송 요청 계약입니다(UC-2, FR-6) — inbound adapter.
 *
 * <p>전체 결과가 응답 코드를 결정합니다:
 * 수신자 없음 404 · 전 채널 성공 200 · 일부 실패 207(Multi-Status) · 전부 실패 502.
 * 발송 여부가 채널마다 갈릴 수 있는 동기 집계라, 부분 실패를 성공(200)으로도
 * 실패(5xx)로도 뭉개지 않고 207로 구분해 외부 시스템이 재시도 범위를 판단하게 합니다.
 *
 * <p>분류(무엇이 부분 실패인가)는 도메인의 {@link DispatchStatus}가 판정하고,
 * 이 어댑터는 그 분류를 HTTP 상태 코드로 번역만 합니다. 실패가 집계로 돌아오는
 * 계약(2026-07-22) 덕분에 207·502 분기가 실제로 도달 가능합니다.
 */
@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    @PostMapping
    public ResponseEntity<DispatchResponse> dispatch(@Valid @RequestBody DispatchRequest request) {
        List<ChannelDispatchResult> results = dispatchNotificationUseCase.dispatch(
                request.groupId(), request.title(), request.content());

        HttpStatus status = switch (DispatchStatus.from(results)) {
            case NO_RECIPIENT -> HttpStatus.NOT_FOUND;
            case ALL_FAILED -> HttpStatus.BAD_GATEWAY;
            case ALL_SUCCEEDED -> HttpStatus.OK;
            case PARTIAL -> HttpStatus.MULTI_STATUS;
        };

        return ResponseEntity.status(status)
                .body(DispatchResponse.of(request.groupId(), results));
    }
}
