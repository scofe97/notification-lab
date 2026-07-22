package com.practice.notification.send.domain.port.out;

import com.practice.notification.common.domain.ChannelType;
import com.practice.notification.send.domain.model.SendResult;
import java.util.List;

/**
 * 채널 발송 out-port입니다.
 *
 * <p>도메인이 "이 목적지들에 이 내용을 발송하라"를 선언하고, infrastructure의
 * {@code ChannelSendAdapter}가 외부 발송 API(Feign + 회로차단기)로 구현합니다.
 * 전송 형식(SendRequest 등)은 어댑터 뒤에 숨습니다.
 *
 * <p><b>실패 계약</b>: 구현은 호출 실패를 예외로 전파하지 않고 실패 집계
 * ({@code SendResult.failure})로 변환해 반환합니다.
 */
public interface ChannelSendPort {

    SendResult send(ChannelType channelType, String title, String content, List<String> destinations);
}
