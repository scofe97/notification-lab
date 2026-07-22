package com.practice.notification.dispatch.domain.model;

import java.util.List;

/**
 * 발송 요청 전체 결과의 분류입니다 — 순수 도메인 판정.
 *
 * <p>채널별 집계를 네 상태로 분류만 하고, 이를 HTTP 상태 코드로 번역하는 일은
 * api 어댑터({@code DispatchController})가 맡습니다.
 */
public enum DispatchStatus {

    NO_RECIPIENT,
    ALL_SUCCEEDED,
    ALL_FAILED,
    PARTIAL;

    public static DispatchStatus from(List<ChannelDispatchResult> results) {
        if (results.isEmpty()) {
            return NO_RECIPIENT;
        }
        if (results.stream().allMatch(ChannelDispatchResult::allFailed)) {
            return ALL_FAILED;
        }
        if (results.stream().allMatch(ChannelDispatchResult::allSucceeded)) {
            return ALL_SUCCEEDED;
        }
        return PARTIAL;
    }
}
