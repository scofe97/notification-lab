package com.practice.notification.dispatch.domain.model;

import com.practice.notification.send.domain.ChannelType;

/**
 * 발송 대상 수신자 한 명입니다 — 순수 도메인 모델.
 *
 * <p>외부 수신자 조회 API가 돌려준 결과를 도메인 언어로 옮긴 것입니다.
 * 조회 API의 응답 형식(JSON 필드명 등)은 infrastructure의 변환이 흡수합니다.
 */
public class Recipient {

    private final String userId;
    private final ChannelType channelType;
    private final String destination;

    public Recipient(String userId, ChannelType channelType, String destination) {
        this.userId = userId;
        this.channelType = channelType;
        this.destination = destination;
    }

    public String getUserId() {
        return userId;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public String getDestination() {
        return destination;
    }
}
