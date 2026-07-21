package com.practice.notification.channel.api;

import jakarta.validation.constraints.NotNull;

/**
 * 채널 설정 저장 요청 바디입니다.
 *
 * <p>userId·channelType은 경로 변수로 받으므로 바디에는 수신 여부만 담습니다.
 *
 * @param enabled 수신 여부
 */
public record ChannelSettingUpdateRequest(
        @NotNull(message = "enabled는 필수입니다") Boolean enabled
) {
}
