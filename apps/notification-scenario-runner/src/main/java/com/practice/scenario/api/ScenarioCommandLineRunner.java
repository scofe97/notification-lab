package com.practice.scenario.api;

import com.practice.scenario.domain.model.ScenarioRequest;
import com.practice.scenario.domain.model.ScenarioType;
import com.practice.scenario.domain.port.in.RunScenarioUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * CLI 원샷 실행 어댑터입니다 — inbound adapter.
 *
 * <p>단계 4(REST 실행 제어) 전까지의 트리거로, args를 파싱해 in-port를 한 번 호출하고
 * 앱을 종료시킵니다({@code main}이 runner 완료 후 exit).
 *
 * <pre>
 * ./gradlew bootRun --args="--scenario=normal --count=10 --interval-ms=500 --channel=sms"
 * ./gradlew bootRun --args="--scenario=poison --count=3"
 * ./gradlew bootRun --args="--scenario=fault-5xx --channel=sms"
 * ./gradlew bootRun --args="--scenario=fault-delay --channel=email --delay-ms=3000"
 * ./gradlew bootRun --args="--scenario=fault-reset"
 * </pre>
 */
@Component
public class ScenarioCommandLineRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScenarioCommandLineRunner.class);

    private final RunScenarioUseCase runScenarioUseCase;

    public ScenarioCommandLineRunner(RunScenarioUseCase runScenarioUseCase) {
        this.runScenarioUseCase = runScenarioUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("scenario")) {
            log.info("사용법: --scenario=normal|burst|poison|fault-5xx|fault-delay|fault-reset "
                    + "[--count=5] [--interval-ms=500] [--channel=sms|email|alimtalk] [--delay-ms=3000]");
            return;
        }

        ScenarioRequest request = new ScenarioRequest(
                ScenarioType.from(option(args, "scenario", "normal"))
                , Integer.parseInt(option(args, "count", "5"))
                , Long.parseLong(option(args, "interval-ms", "500"))
                , option(args, "channel", "sms").toLowerCase()
                , Long.parseLong(option(args, "delay-ms", "3000"))
        );
        runScenarioUseCase.run(request);
    }

    private String option(ApplicationArguments args, String name, String defaultValue) {
        if (!args.containsOption(name) || args.getOptionValues(name).isEmpty()) {
            return defaultValue;
        }
        return args.getOptionValues(name).get(0);
    }
}
