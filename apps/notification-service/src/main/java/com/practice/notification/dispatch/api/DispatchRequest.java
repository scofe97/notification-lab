package com.practice.notification.dispatch.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 외부 발송 요청 바디입니다.
 *
 * @param groupId 수신자 그룹 ID — 수신자 조회 API의 조회 키
 * @param title   알림 제목
 * @param content 알림 본문
 */
public record DispatchRequest(
        @NotBlank(message = "groupId는 필수입니다") String groupId,
        @NotBlank(message = "title은 필수입니다") String title,
        @NotBlank(message = "content는 필수입니다") String content
) {
}
