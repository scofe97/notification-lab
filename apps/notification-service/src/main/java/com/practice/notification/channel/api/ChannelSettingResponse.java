package com.practice.notification.channel.api;

import com.practice.notification.common.domain.ChannelType;

/**
 * 채널 설정 조회 응답입니다(FR-12).
 *
 * @param userId      수신자 ID
 * @param channelType 채널
 * @param enabled     수신 여부. 저장된 설정이 없으면 기본값 {@code true}
 */
public record ChannelSettingResponse(
        String userId,
        ChannelType channelType,
        boolean enabled
) {

    public static ChannelSettingResponse of(String userId, ChannelType channelType, boolean enabled) {
        return new ChannelSettingResponse(userId, channelType, enabled);
    }
}
