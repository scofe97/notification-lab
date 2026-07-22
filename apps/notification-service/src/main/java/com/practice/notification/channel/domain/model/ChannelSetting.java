package com.practice.notification.channel.domain.model;

import com.practice.notification.common.domain.ChannelType;

/**
 * 수신자의 채널별 수신 설정입니다 — 순수 도메인 모델.
 *
 * <p>발송 파이프라인은 이 설정을 조회해 "수신 거부한 채널로는 안 보내는" 판단을 합니다.
 * 자주 바뀌지 않으므로 조회 결과를 Caffeine으로 캐시합니다(FR-3).
 *
 * <p>영속 매핑은 infrastructure의 {@code ChannelSettingEntity}가 맡습니다. 이 클래스는
 * 프레임워크 어노테이션 없이 도메인 사실(누가·어느 채널을·수신하는가)만 담습니다.
 * {@link ChannelType}은 발송 컨텍스트와 공유하는 커널로 {@code send.domain}에 둡니다.
 */
public class ChannelSetting {

    private final String userId;
    private final ChannelType channelType;
    private final boolean enabled;

    public ChannelSetting(String userId, ChannelType channelType, boolean enabled) {
        this.userId = userId;
        this.channelType = channelType;
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
