package com.practice.notification.dispatch.domain.port.out;

import com.practice.notification.dispatch.domain.model.Recipient;
import java.util.List;

/**
 * 수신자 조회 out-port입니다.
 *
 * <p>도메인이 "그룹의 발송 대상을 알려달라"는 요구를 선언하고,
 * infrastructure의 Feign 어댑터가 외부 조회 API(WireMock 모사)로 구현합니다.
 */
public interface RecipientLookupPort {

    /** 그룹에 속한 수신자 목록을 조회합니다. 없으면 빈 목록입니다. */
    List<Recipient> findByGroup(String groupId);
}
