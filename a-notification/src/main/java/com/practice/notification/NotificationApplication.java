package com.practice.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 원본 알림 서비스(사내 알림 서비스)의 발송 파이프라인을 오픈소스로 축소 재현한 앱입니다.
 *
 * <p>원본 진입점은 {@code @EnableFeignClients}·{@code @EnableJpaAuditing}·
 * {@code @EnableScheduling}·{@code @EnableEncryptableProperties}를 선언합니다. 이 미니 프로젝트는
 * 발송(Feign)·설정 캐시(Caching)에 필요한 것만 켭니다. 스케줄러·Jasypt는 a2/축 B에서 다룹니다.
 */
@SpringBootApplication
@EnableFeignClients
@EnableCaching
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
