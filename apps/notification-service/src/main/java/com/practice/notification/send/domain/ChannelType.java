package com.practice.notification.send.domain;

/**
 * 알림 채널 종류입니다. 수신자는 채널별로 그룹핑해 각기 다른 발송 경로로 전달합니다.
 */
public enum ChannelType {
    SMS,
    ALIMTALK,
    EMAIL
}
