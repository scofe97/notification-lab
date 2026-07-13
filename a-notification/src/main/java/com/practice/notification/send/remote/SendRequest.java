package com.practice.notification.send.remote;

import java.util.List;

/**
 * 외부 발송 API 요청 바디입니다. 채널 하나에 대한 발송 요청을 담습니다.
 *
 * <p>원본은 {@code NcpUtil}이 HMAC-SHA256 서명을 붙여 NCP SENS로 보내지만, 이 미니 프로젝트는
 * 목(WireMock) 대상이라 서명 없이 목적지·내용만 보냅니다.
 *
 * @param title        알림 제목
 * @param content      알림 본문
 * @param destinations 목적지 목록 (전화번호 또는 이메일)
 */
public record SendRequest(
        String title,
        String content,
        List<String> destinations
) {
}
