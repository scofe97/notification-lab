package com.practice.notification.dispatch.infrastructure.recipient;

import com.practice.notification.dispatch.domain.model.Recipient;
import com.practice.notification.send.domain.ChannelType;

/**
 * 수신자 조회 API의 응답 한 건입니다 — 어댑터 내부 전송 모델.
 *
 * <p>외부 응답 형식은 여기까지만 오고, 도메인에는 {@link Recipient}로 변환해 넘깁니다.
 *
 * @param userId      수신자 ID
 * @param channelType 수신 채널 (SMS/ALIMTALK/EMAIL)
 * @param destination 목적지 (전화번호/이메일)
 */
public record RecipientResponse(
        String userId,
        ChannelType channelType,
        String destination
) {

    Recipient toDomain() {
        return new Recipient(userId, channelType, destination);
    }
}
