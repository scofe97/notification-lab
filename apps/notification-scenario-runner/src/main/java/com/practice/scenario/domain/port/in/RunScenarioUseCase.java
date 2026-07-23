package com.practice.scenario.domain.port.in;

import com.practice.scenario.domain.model.ScenarioRequest;

/**
 * 시나리오 실행 유스케이스(in-port)입니다.
 *
 * <p>CLI 어댑터(단계 4에서 REST 어댑터 추가 예정)가 이 포트로 진입합니다.
 */
public interface RunScenarioUseCase {

    void run(ScenarioRequest request);
}
