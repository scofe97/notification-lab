package com.practice.notification.send.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.notification.send.domain.NotificationEvent;
import com.practice.notification.send.domain.SendResult;
import com.practice.notification.send.service.NotificationSendService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka {@code notification} 토픽을 소비하는 리스너입니다.
 *
 * <p>표준 {@code @KafkaListener}는 "토픽 구독 → 역직렬화 → 핸들러 호출"을 담당합니다.
 *
 * <p>메시지는 JSON String으로 받아 {@link NotificationEvent}로 역직렬화합니다.
 * 처리 중 예외가 나면 리스너 컨테이너의 에러 핸들러가 재시도 후 DLT로 보냅니다(FR-5). 그래서 여기서는
 * 예외를 잡지 않고 그대로 던집니다 — 잡아버리면 실패가 성공으로 커밋돼 메시지를 잃습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationSendService sendService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notification.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) throws Exception {
        // 역직렬화 실패도 예외로 전파 → 에러 핸들러가 DLT로 (독약 메시지 격리)
        NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
        log.debug("알림 이벤트 수신: eventId={}, 수신자 {}명", event.eventId(), event.receivers().size());

        List<SendResult> results = sendService.send(event);
        log.info("발송 완료: eventId={}, 채널별 결과={}", event.eventId(), results);
    }
}
