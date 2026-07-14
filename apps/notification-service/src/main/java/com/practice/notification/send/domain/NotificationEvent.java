package com.practice.notification.send.domain;

import java.util.List;

/**
 * Kafka {@code notification} 토픽으로 들어오는 알림 이벤트입니다.
 *
 * <p>이 프로젝트는 String(JSON) 직렬화를 쓰므로 리스너가 JSON을 이 record로 역직렬화합니다.
 *
 * @param eventId   이벤트 식별자 (멱등 처리·추적용)
 * @param title     알림 제목
 * @param content   알림 본문
 * @param receivers 수신자 목록 (채널타입 포함)
 */
public record NotificationEvent(
        String eventId,
        String title,
        String content,
        List<Receiver> receivers
) {

    /**
     * 수신자 한 명. 어떤 채널로 받을지({@link ChannelType})와 목적지(전화번호/이메일)를 가집니다.
     *
     * @param userId      수신자 ID (채널 설정 조회 키)
     * @param channelType 수신 채널
     * @param destination 목적지 (SMS/ALIMTALK=전화번호, EMAIL=이메일 주소)
     */
    public record Receiver(
            String userId,
            ChannelType channelType,
            String destination
    ) {
    }
}
