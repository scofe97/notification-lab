package com.practice.scenario.infrastructure.wiremock;

import com.practice.scenario.domain.port.out.FaultInjectionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * {@link FaultInjectionPort}의 WireMock 구현체입니다 — outbound adapter.
 *
 * <p>런타임 스텁을 admin API로 주입합니다. priority 1이라 파일 스텁(정상 200)보다 우선하고,
 * {@code reset}이 파일 스텁 상태로 복원합니다. 스텁은 메모리에만 있어 WireMock 재시작으로도 사라집니다.
 */
@Component
public class WireMockFaultAdapter implements FaultInjectionPort {

    private static final Logger log = LoggerFactory.getLogger(WireMockFaultAdapter.class);

    private final RestClient restClient;

    public WireMockFaultAdapter(@Value("${scenario.wiremock.admin-url}") String adminUrl) {
        this.restClient = RestClient.create(adminUrl);
    }

    @Override
    public void inject5xx(String channel) {
        String stub = """
                {"priority": 1,
                 "request": {"method": "POST", "urlPath": "/send/%s"},
                 "response": {"status": 500, "jsonBody": {"error": "injected-by-scenario-runner"}}}
                """.formatted(channel);
        registerStub(stub);
        log.info("장애 주입: /send/{} → 500", channel);
    }

    @Override
    public void injectDelay(String channel, long delayMs) {
        String stub = """
                {"priority": 1,
                 "request": {"method": "POST", "urlPath": "/send/%s"},
                 "response": {"status": 200,
                              "fixedDelayMilliseconds": %d,
                              "jsonBody": {"statusCode": "202", "statusName": "delayed", "requestId": "scn-delay"}}}
                """.formatted(channel, delayMs);
        registerStub(stub);
        log.info("지연 주입: /send/{} → {}ms", channel, delayMs);
    }

    @Override
    public void reset() {
        restClient.post().uri("/__admin/mappings/reset").retrieve().toBodilessEntity();
        log.info("WireMock 원상복구 완료 (파일 스텁 기본값)");
    }

    private void registerStub(String stubJson) {
        restClient.post()
                .uri("/__admin/mappings")
                .header("Content-Type", "application/json")
                .body(stubJson)
                .retrieve()
                .toBodilessEntity();
    }
}
