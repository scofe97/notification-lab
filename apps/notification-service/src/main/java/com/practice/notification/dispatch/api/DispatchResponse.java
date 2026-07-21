package com.practice.notification.dispatch.api;

import com.practice.notification.dispatch.domain.model.ChannelDispatchResult;
import java.util.List;

/**
 * 외부 발송 요청 응답입니다. 채널별 집계를 그대로 노출합니다.
 *
 * @param groupId 요청한 그룹 ID
 * @param results 채널별 발송 결과 (수신자 없음이면 빈 목록)
 */
public record DispatchResponse(
        String groupId,
        List<ChannelResult> results
) {

    /** 채널 하나의 집계입니다. */
    public record ChannelResult(String channelType, int requested, int succeeded, int failed) {
    }

    public static DispatchResponse of(String groupId, List<ChannelDispatchResult> results) {
        List<ChannelResult> converted = results.stream()
                .map(r -> new ChannelResult(r.channelType().name(), r.requested(), r.succeeded(), r.failed()))
                .toList();

        return new DispatchResponse(groupId, converted);
    }
}
