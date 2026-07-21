package com.practice.notification.dispatch.infrastructure;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 수신자 조회 API를 호출하는 선언적 HTTP 클라이언트입니다.
 *
 * <p>base-url은 {@code notification.recipient.base-url} (기본 WireMock: http://localhost:8110).
 * WireMock 스텁 {@code GET /recipients/{groupId}}에 매칭됩니다.
 */
@FeignClient(name = "recipientLookup", url = "${notification.recipient.base-url}")
public interface RecipientLookupClient {

    @GetMapping("/recipients/{groupId}")
    List<RecipientResponse> findByGroup(@PathVariable("groupId") String groupId);
}
