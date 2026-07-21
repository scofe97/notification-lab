package com.practice.notification.dispatch.domain.port.in;

import com.practice.notification.dispatch.domain.model.ChannelDispatchResult;
import java.util.List;

/**
 * 외부 REST 발송 요청 유스케이스(in-port)입니다(UC-2, FR-6).
 *
 * <p>REST 어댑터가 이 포트로 진입합니다. 구현은 application의 {@code DispatchService}입니다.
 */
public interface DispatchNotificationUseCase {

    /**
     * 그룹의 수신자를 조회해 채널별로 발송하고 결과를 집계합니다.
     * 그룹에 수신자가 없으면 빈 목록을 돌려줍니다.
     */
    List<ChannelDispatchResult> dispatch(String groupId, String title, String content);
}
