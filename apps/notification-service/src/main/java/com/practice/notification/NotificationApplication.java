package com.practice.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kafka 알림 발송 파이프라인을 학습·관측하는 앱입니다.
 *
 * <p>발송(Feign)·설정 캐시(Caching)에 필요한 기능만 활성화합니다. 스케줄러와 암호화는 현재 범위 밖입니다.
 */
@SpringBootApplication
@EnableFeignClients
@EnableCaching
@EnableScheduling
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
