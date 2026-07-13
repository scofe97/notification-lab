package com.practice.notification.send.remote;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 외부 발송 API를 호출하는 선언적 HTTP 클라이언트입니다.
 *
 * <p>원본의 {@code NotificationApiClient}(Feign)는 URL 인코딩 이슈로 {@code @Deprecated}이고
 * 실제 발송은 {@code NcpUtil}의 직접 HTTP로 이뤄집니다. 이 미니 프로젝트는 <b>OpenFeign 학습</b>이
 * 목적이라 Feign을 정상 경로로 되살렸습니다. 인터페이스 선언만으로 HTTP 클라이언트가 만들어지는 것을
 * 체득하는 것이 핵심입니다.
 *
 * <p>base-url은 {@code notification.send.base-url} (기본 WireMock: http://localhost:8110).
 */
@FeignClient(name = "notificationSend", url = "${notification.send.base-url}")
public interface NotificationSendClient {

    /**
     * 채널별 발송을 요청합니다. 경로의 {@code channel}로 sms/alimtalk/email을 구분합니다.
     * WireMock 스텁 {@code POST /send/(sms|alimtalk|email)}에 매칭됩니다.
     */
    @PostMapping("/send/{channel}")
    SendResponse send(@PathVariable("channel") String channel, @RequestBody SendRequest request);
}
