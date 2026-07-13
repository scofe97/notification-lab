package com.practice.notification.send.remote;

/**
 * 외부 발송 API 응답입니다. WireMock 스텁이 돌려주는 형태에 맞춥니다.
 *
 * @param statusCode 발송 상태 코드 (예: "202")
 * @param statusName 상태 이름 (예: "success")
 * @param requestId  발송 요청 식별자
 */
public record SendResponse(
        String statusCode,
        String statusName,
        String requestId
) {
}
