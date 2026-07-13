package com.practice.notification.send.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 소비 실패 처리 설정입니다. 원본 사내 Kafka 라이브러리의 {@code FailurePolicy.DLQ}에 대응합니다(FR-5).
 *
 * <p><b>동작</b>: 리스너가 예외를 던지면 {@link DefaultErrorHandler}가 정해진 횟수만큼 재시도하고,
 * 그래도 실패하면 {@link DeadLetterPublishingRecoverer}가 원본 메시지를 <b>{원본토픽}.DLT</b> 토픽으로
 * 보냅니다. 이렇게 처리 불가 메시지(독약 메시지)를 격리해, 무한 재소비로 컨슈머가 막히는 것을 막습니다.
 *
 * <p>재시도는 2회(초기 시도 + 재시도 2 = 총 3회), 간격 1초로 잡았습니다. 학습용이라 짧게 뒀습니다.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // 실패 메시지를 {원본토픽}.DLT 로 라우팅
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        record.topic() + ".DLT", record.partition()));

        // 재시도 2회, 1초 간격. 소진되면 recoverer가 DLT로 보냄
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
