package com.practice.scenario.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.scenario.domain.port.out.EventPublishPort;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * {@link EventPublishPort}의 Kafka 구현체입니다 — outbound adapter.
 *
 * <p>notification-service의 {@code NotificationEvent} record와 호환되는 JSON을 조립합니다
 * (코드 의존 없음 — 별개 프로젝트라 스키마를 계약으로 복제).
 * CLI 원샷 앱이라 발행은 동기(get)로 확정하고 종료합니다 — flush 유실 방지.
 */
@Component
public class KafkaEventPublishAdapter implements EventPublishPort {

    private static final Map<String, String> DESTINATIONS = Map.of(
            "sms", "01000000100"
            , "email", "runner@example.local"
            , "alimtalk", "01000000100"
    );

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaEventPublishAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper,
                                    @Value("${scenario.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public String publishNormal(String channel) {
        String eventId = "scn-" + UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId
                , "title", "scenario-runner"
                , "content", "normal traffic"
                , "receivers", List.of(Map.of(
                        "userId", "scn-user-1"
                        , "channelType", channel.toUpperCase()
                        , "destination", DESTINATIONS.getOrDefault(channel, "01000000100")))
        );
        send(toJson(event));

        return eventId;
    }

    @Override
    public String publishPoison(int variant) {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        String payload;
        String description;
        switch (variant) {
            case 0 -> {
                payload = "poison-not-json-" + marker;
                description = "JSON 아닌 문자열 (" + marker + ")";
            }
            case 1 -> {
                payload = toJson(Map.of("eventId", "scn-poison-" + marker, "title", "poison"));
                description = "receivers 누락 JSON (scn-poison-" + marker + ")";
            }
            default -> {
                payload = toJson(Map.of(
                        "eventId", "scn-poison-" + marker
                        , "title", "poison"
                        , "content", "unsupported channel"
                        , "receivers", List.of(Map.of(
                                "userId", "scn-user-1", "channelType", "FAX", "destination", "0100"))));
                description = "미지원 channelType=FAX (scn-poison-" + marker + ")";
            }
        }
        send(payload);

        return description;
    }

    private void send(String payload) {
        try {
            kafkaTemplate.send(topic, payload).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("발행 대기 중 인터럽트", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Kafka 발행 실패: " + e.getMessage(), e);
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이벤트 JSON 조립 실패", e);
        }
    }
}
