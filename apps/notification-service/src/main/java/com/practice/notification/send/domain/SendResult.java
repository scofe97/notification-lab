package com.practice.notification.send.domain;

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

    public boolean allSucceeded() {
        return failed == 0;
    }
}
