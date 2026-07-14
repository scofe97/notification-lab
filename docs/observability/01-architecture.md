# LGMT Observability 아키텍처

`notification-service`는 관측 대상이고, `notification-scenario-runner`는 장애를 재현하는 발생기입니다. LGMT는 증거를 저장하고, Grafana는 그 증거를 탐색하는 관제 화면입니다.

## 전체 미들웨어 배치

```mermaid
flowchart TB
    SR["Notification Scenario Runner<br/>부하 생성 · 장애 주입"]
    K["Kafka<br/>notification · retry · DLT"]
    APP["Notification Service"]
    DB[("MariaDB")]
    EXT["WireMock"]
    OBS["Actuator · Micrometer · OTel · Alloy"]
    LGMT["Loki · Mimir · Tempo · Grafana"]
    SR --> K --> APP
    APP --> DB
    APP --> EXT
    SR -. "scenario history" .-> OBS
    APP -. "logs · metrics · traces" .-> OBS --> LGMT
    style SR fill:#cce5ff,color:#000
    style K fill:#fff3cd,color:#000
    style APP fill:#d4edda,color:#000
    style DB fill:#e2d9f3,color:#000
    style EXT fill:#cce5ff,color:#000
    style OBS fill:#ffe5d0,color:#000
    style LGMT fill:#f8d7da,color:#000
```

## Kafka 처리·재시도·DLT

현재 구현은 인메모리 2회 재시도 뒤 DLT로 보냅니다. `notification.retry`는 Phase 3에서 추가할 목표 상태이며 점선으로 표시합니다.

```mermaid
flowchart TB
    P["Scenario Runner"] --> T["notification"] --> C["NotificationListener"] --> S["NotificationSendService"]
    S -->|"성공"| OK["처리 완료"]
    S -->|"현재: 재시도 2회"| C
    C -->|"현재: 소진"| DLT["notification.DLT"]
    S -. "목표: 일시 실패" .-> R["notification.retry"]
    R -. "재시도 소비" .-> C
    R -. "소진" .-> DLT
    style T fill:#fff3cd,color:#000
    style R fill:#fff3cd,color:#000
    style DLT fill:#f8d7da,color:#000
    style OK fill:#d4edda,color:#000
```

## Notification Service 내부 흐름

```mermaid
flowchart TB
    L["NotificationListener"] --> V["Deserialize · Validation"] --> S["NotificationSendService"]
    S --> C{"Caffeine cache"}
    C -->|"hit"| CB["CircuitBreaker"]
    C -->|"miss"| DB[("MariaDB 조회")] --> CB
    CB --> F["OpenFeign"] --> W["WireMock"]
    W -->|"200"| OK["발송 성공"]
    W -->|"5xx · timeout"| FAIL["재시도 또는 DLT"]
    CB -->|"OPEN"| FAIL
    style L fill:#d4edda,color:#000
    style DB fill:#e2d9f3,color:#000
    style W fill:#cce5ff,color:#000
    style FAIL fill:#f8d7da,color:#000
```

## LGMT 관측 파이프라인

```mermaid
flowchart TB
    APP["Notification Service"] --> ACT["Actuator · Micrometer"] --> COL["Grafana Alloy"]
    APP --> OTEL["OpenTelemetry"] --> COL
    RUN["Scenario Runner"] --> OTEL
    COL --> L["Loki logs"]
    COL --> M["Mimir metrics"]
    COL --> T["Tempo traces"]
    L --> G["Grafana"]
    M --> G
    T --> G
    style APP fill:#d4edda,color:#000
    style RUN fill:#cce5ff,color:#000
    style COL fill:#ffe5d0,color:#000
```

## 주차별 발전 아키텍처

### 1주차 — 관측 설계와 정상 기준선

```mermaid
flowchart LR
    K["Kafka"] --> APP["Notification Service"]
    APP --> H2[("H2")]
    APP --> W["WireMock"]
    APP -. "설계할 신호" .-> SC["Signal Catalog"]
    style APP fill:#d4edda,color:#000
    style SC fill:#ffe5d0,color:#000
```

### 2주차 — 계측과 LGMT 연결

```mermaid
flowchart LR
    APP["Notification Service<br/>Actuator · Micrometer · OTel"] --> ALLOY["Alloy"]
    APP --> DB[("MariaDB")]
    ALLOY --> LGMT["Loki · Mimir · Tempo · Grafana"]
    style APP fill:#d4edda,color:#000
    style DB fill:#e2d9f3,color:#000
    style ALLOY fill:#ffe5d0,color:#000
```

### 3주차 — 장애 주입과 증거 수집

```mermaid
flowchart LR
    SR["Scenario Runner<br/>normal · burst · poison"] --> K["Kafka"] --> APP["Notification Service"]
    SR -. "200 · 5xx · delay" .-> W["WireMock"]
    APP --> W
    APP --> R["retry · DLT"]
    APP -. "telemetry" .-> G["Grafana Explore"]
    SR -. "scenario history" .-> G
    style SR fill:#cce5ff,color:#000
    style APP fill:#d4edda,color:#000
    style R fill:#f8d7da,color:#000
```

### 4주차 — 운영 결과물화

```mermaid
flowchart LR
    SIG["LGMT signals"] --> DASH["Grafana dashboards"] --> ALERT["Alert rules"] --> RB["Runbook"] --> EXP["Experiment record"]
    EXP --> DASH
    style SIG fill:#f8d7da,color:#000
    style DASH fill:#d4edda,color:#000
    style ALERT fill:#fff3cd,color:#000
    style RB fill:#cce5ff,color:#000
```
