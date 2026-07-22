package com.practice.notification.send.infrastructure.sendapi;

import java.util.List;

/**
 * 외부 발송 API 요청 바디입니다. 채널 하나에 대한 발송 요청을 담습니다.
 *
 * <p>이 프로젝트는 WireMock으로 외부 발송 API를 모사하므로, 목적지·내용만 보냅니다.
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
