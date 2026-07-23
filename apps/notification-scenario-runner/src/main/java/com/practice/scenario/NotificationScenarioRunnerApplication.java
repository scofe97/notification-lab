package com.practice.scenario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Service 관측 실험의 데이터·장애 발생기 진입점입니다.
 *
 * <p>CLI 원샷 앱입니다 — {@code --scenario=...} 인자로 시나리오를 한 번 실행하고 종료합니다
 * (runner 완료 후 컨텍스트를 닫고 exit). 상주형 REST 제어는 단계 4에서 추가합니다.
 */
@SpringBootApplication
public class NotificationScenarioRunnerApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
                SpringApplication.run(NotificationScenarioRunnerApplication.class, args)));
    }
}
