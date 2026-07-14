package com.practice.scenario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Service 관측 실험의 데이터·장애 발생기 진입점입니다.
 *
 * <p>시나리오별 Kafka 발행, WireMock 모드 전환, scenario history 기록은 Phase 3 구현에서 추가합니다.
 */
@SpringBootApplication
public class NotificationScenarioRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationScenarioRunnerApplication.class, args);
    }
}
