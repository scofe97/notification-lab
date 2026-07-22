package com.practice.notification.send.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.notification.send.domain.model.NotificationEvent;
import com.practice.notification.send.domain.model.SendResult;
import com.practice.notification.send.domain.port.in.SendNotificationUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka {@code notification} 토픽을 소비하는 리스너입니다 — inbound adapter.
 *
 * <p>메시지는 JSON String으로 받아 {@link NotificationEvent}로 역직렬화하고, in-port로 발송을 요청합니다.
 * 역직렬화 실패는 예외로 전파돼 에러 핸들러가 재시도 후 DLT로 보냅니다(독약 메시지 격리, FR-5).
 *
 * <p><b>Kafka 입구의 실패 의미론</b>: in-port는 발송 실패를 예외가 아닌 집계로 돌려줍니다.
 * 이 어댑터가 실패 집계를 <b>예외로 번역</b>해 재시도→DLT를 발동시킵니다 — 잡거나 삼키면
 * 실패가 성공으로 커밋돼 메시지를 잃습니다. 같은 집계를 REST 입구(dispatch)는 207/502로 번역합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notification.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) throws Exception {
        NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
        log.debug("알림 이벤트 수신: eventId={}, 수신자 {}명", event.eventId(), event.receivers().size());

        List<SendResult> results = sendNotificationUseCase.send(event);

        List<SendResult> failures = results.stream()
                .filter(r -> r.failed() > 0)
                .toList();
        if (!failures.isEmpty()) {
            // 실패 집계 → 예외 번역: 리스너 컨테이너의 에러 핸들러가 재시도 후 DLT로 보낸다
            throw new IllegalStateException("발송 실패 채널 존재: eventId=" + event.eventId() + ", " + failures);
        }

        log.info("발송 완료: eventId={}, 채널별 결과={}", event.eventId(), results);
    }
}
