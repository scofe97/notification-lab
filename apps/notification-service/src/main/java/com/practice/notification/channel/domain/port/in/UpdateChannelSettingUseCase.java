package com.practice.notification.channel.domain.port.in;

import com.practice.notification.send.domain.ChannelType;

/**
 * 채널 수신 설정 저장 유스케이스(in-port)입니다(UC-4, FR-12).
 */
public interface UpdateChannelSettingUseCase {

    /**
     * 수신 설정을 저장하고(없으면 생성, 있으면 덮어쓰는 upsert) 저장된 수신 여부를 돌려줍니다.
     */
    boolean update(String userId, ChannelType channelType, boolean enabled);
}
