package com.practice.notification.send.domain.port.in;

import com.practice.notification.send.domain.model.NotificationEvent;
import com.practice.notification.send.domain.model.SendResult;
import java.util.List;

/**
 * 알림 발송 유스케이스(in-port)입니다(UC-1·UC-2 공용, FR-2~5).
 *
 * <p>Kafka 리스너(UC-1)와 dispatch의 완충 어댑터(UC-2)가 이 포트로 진입합니다.
 * 구현은 application의 {@code NotificationSendService}입니다.
 *
 * <p><b>실패 계약</b>: 발송 실패는 예외가 아니라 {@link SendResult}의 실패 집계로 반환합니다.
 * 실패를 어떻게 해석할지는 입구가 정합니다 — Kafka 리스너는 실패 집계를 예외로 번역해
 * 재시도·DLT를 발동시키고, REST 입구는 207/502 응답 코드로 번역합니다.
 */
public interface SendNotificationUseCase {

    /** 이벤트의 수신자를 채널별로 그룹핑해 발송하고, 채널별 집계를 돌려줍니다. */
    List<SendResult> send(NotificationEvent event);
}
