package com.practice.notification.history.domain.port.in;

import com.practice.notification.common.domain.ChannelType;
import java.util.List;

/**
 * 발송 이력 기록 유스케이스(in-port)입니다(FR-8).
 *
 * <p>send 컨텍스트가 채널 발송을 마친 직후 호출합니다.
 *
 * <p><b>best-effort 계약</b>: 이력 기록 실패는 절대 호출자에게 전파하지 않습니다(경고 로그만).
 * 기록 실패가 예외로 새면 Kafka 입구에서 재시도가 돌아 이미 성공한 발송이 중복되기 때문입니다
 * (2026-07-22 UC-2 실측의 교훈). 이력은 발송의 부가 관심사이지 성공 조건이 아닙니다.
 */
public interface RecordNotificationHistoryUseCase {

    /** 채널 하나의 발송 결과를 목적지별 이력으로 기록합니다. */
    void record(String eventId, ChannelType channelType, List<String> destinations, boolean succeeded);
}
