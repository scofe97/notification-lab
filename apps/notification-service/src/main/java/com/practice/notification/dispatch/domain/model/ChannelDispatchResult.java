package com.practice.notification.dispatch.domain.model;

import com.practice.notification.common.domain.ChannelType;

/**
 * 채널 하나의 발송 결과 집계입니다 — 순수 도메인 모델.
 *
 * <p>send 컨텍스트의 {@code SendResult}와 형태가 같지만 별도 타입으로 둡니다.
 * dispatch 도메인이 send 내부 타입에 묶이지 않게 하는 변환 경계이며,
 * 두 컨텍스트가 각자 진화할 수 있게 합니다(변환은 infrastructure가 수행).
 */
public record ChannelDispatchResult(
        ChannelType channelType,
        int requested,
        int succeeded,
        int failed
) {

    public boolean allSucceeded() {
        return failed == 0;
    }

    public boolean allFailed() {
        return requested > 0 && succeeded == 0;
    }
}
