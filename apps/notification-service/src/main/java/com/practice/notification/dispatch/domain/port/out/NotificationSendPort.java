package com.practice.notification.dispatch.domain.port.out;

import com.practice.notification.dispatch.domain.model.ChannelDispatchResult;
import com.practice.notification.dispatch.domain.model.Recipient;
import java.util.List;

/**
 * 알림 발송 out-port입니다.
 *
 * <p>dispatch 도메인은 "이 수신자들에게 이 내용을 발송하라"만 요구하고,
 * 실제 발송(채널 그룹핑·설정 필터·회로차단기)은 어댑터가 send 컨텍스트의
 * 기존 발송 서비스에 위임합니다. send가 아직 in-port가 없는 레거시 구조라,
 * 이 out-port가 두 컨텍스트 사이의 완충(ACL) 역할을 합니다.
 */
public interface NotificationSendPort {

    /** 수신자들에게 발송하고 채널별 집계를 돌려줍니다. */
    List<ChannelDispatchResult> send(String title, String content, List<Recipient> recipients);
}
