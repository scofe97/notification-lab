package com.practice.scenario.application;

import com.practice.scenario.domain.model.ScenarioRequest;
import com.practice.scenario.domain.port.in.RunScenarioUseCase;
import com.practice.scenario.domain.port.out.EventPublishPort;
import com.practice.scenario.domain.port.out.FaultInjectionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 시나리오 실행 유스케이스 구현(application 계층)입니다.
 *
 * <p>발행 계열은 EventPublishPort로, 장애 계열은 FaultInjectionPort로 위임하는
 * 오케스트레이션만 합니다. Runner는 실패 원인을 판정하지 않습니다 — 신호를 만들고,
 * 해석은 notification-service의 metric·log·trace와 Grafana가 합니다.
 */
@Service
public class ScenarioService implements RunScenarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScenarioService.class);

    private final EventPublishPort eventPublishPort;
    private final FaultInjectionPort faultInjectionPort;

    public ScenarioService(EventPublishPort eventPublishPort, FaultInjectionPort faultInjectionPort) {
        this.eventPublishPort = eventPublishPort;
        this.faultInjectionPort = faultInjectionPort;
    }

    @Override
    public void run(ScenarioRequest request) {
        log.info("시나리오 시작: {}", request);
        switch (request.type()) {
            case NORMAL -> publishLoop(request.count(), request.intervalMs(), request.channel());
            case BURST -> publishLoop(request.count(), 0L, request.channel());
            case POISON -> publishPoison(request.count());
            case FAULT_5XX -> faultInjectionPort.inject5xx(request.channel());
            case FAULT_DELAY -> faultInjectionPort.injectDelay(request.channel(), request.delayMs());
            case FAULT_RESET -> faultInjectionPort.reset();
        }
        log.info("시나리오 종료: type={}", request.type());
    }

    private void publishLoop(int count, long intervalMs, String channel) {
        for (int i = 0; i < count; i++) {
            String eventId = eventPublishPort.publishNormal(channel);
            log.info("발행 {}/{}: eventId={}", i + 1, count, eventId);
            sleep(intervalMs);
        }
    }

    private void publishPoison(int count) {
        for (int i = 0; i < count; i++) {
            String description = eventPublishPort.publishPoison(i % 3);
            log.info("독약 발행 {}/{}: {}", i + 1, count, description);
        }
    }

    private void sleep(long intervalMs) {
        if (intervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("발행 간격 대기 중 인터럽트", e);
        }
    }
}
