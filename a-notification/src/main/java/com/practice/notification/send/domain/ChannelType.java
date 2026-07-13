package com.practice.notification.send.domain;

/**
 * 알림 채널 종류입니다. 원본은 수신자를 이 채널별로 그룹핑해 각기 다른 NCP API로 발송합니다.
 * (원본 §5.1 ⑤ "수신자를 SMS / ALIMTALK / EMAIL 채널별 그룹핑")
 */
public enum ChannelType {
    SMS,
    ALIMTALK,
    EMAIL
}
