package com.practice.notification.send.domain.model;

import com.practice.notification.common.domain.ChannelType;

/**
 * 채널별 발송 결과입니다.
 *
 * @param channelType 발송 채널
 * @param requested   요청 건수
 * @param succeeded   성공 건수
 * @param failed      실패 건수
 */
public record SendResult(
        ChannelType channelType,
        int requested,
        int succeeded,
        int failed
) {
    public static SendResult of(ChannelType channelType, int requested, int succeeded) {
        return new SendResult(channelType, requested, succeeded, requested - succeeded);
    }

    /** 채널 전체가 실패했을 때의 집계입니다. 어댑터가 발송 예외를 이 값으로 변환합니다. */
    public static SendResult failure(ChannelType channelType, int requested) {
        return new SendResult(channelType, requested, 0, requested);
    }

    public boolean allSucceeded() {
        return failed == 0;
    }
}
