package com.practice.notification.channel.domain.port.in;

import com.practice.notification.common.domain.ChannelType;

/**
 * 채널 수신 여부 조회 유스케이스(in-port)입니다.
 *
 * <p>REST 조회(UC-4)와 발송 파이프라인의 수신거부 필터(UC-1)가 이 포트로 진입합니다.
 * 구현은 application 계층의 {@code ChannelSettingService}입니다.
 */
public interface GetChannelSettingUseCase {

    /**
     * 수신자가 해당 채널을 수신하도록 설정했는지 조회합니다.
     * 설정이 없으면 기본값 {@code true}(수신)로 간주합니다.
     */
    boolean isEnabled(String userId, ChannelType channelType);
}
