# Prometheus 스택 실습 — 02-01 인출 미달 축 교정

2026-07-28 · 《Mastering Prometheus》 2장(Prometheus 배포) Phase 3 실습 기록입니다.

Phase 1 인출에서 다섯 축 중 둘이 미달이고 하나가 미해결로 남았습니다. 미달 둘의 뿌리는 **층 혼동**입니다 — 프로세스 층과 부품 층을 섞었고, Prometheus 스택과 LGTM 스택을 섞었습니다. 산문으로 다시 읽는 대신 **프로세스를 각각 띄워 보는 것**으로 교정합니다.

| 인출 항목 | 결과 | 이 실습에서 겨누는 곳 |
|---|---|---|
| §1 스택 4구성요소 | ❌ LGTM 으로 답 | A-4 |
| §2 내부 4부품 | ❌ Alertmanager·exporter 로 답 | A-1·A-2 |
| §3 Alertmanager 분리 | △ 병목을 기계로 오답 | B |
| §4 단명 프로세스 | ✅ 자력 통과 | C-1·C-2 |
| ⭐ 미해결 물음 | — "파일 쓰기가 결국 push 와 다를 게 있는가" | **C-5** |

기록 규약은 항목마다 **예측 → 실제 → 해석** 셋입니다. 예측을 먼저 적지 않으면 결과를 보고 "알고 있었다"고 착각하게 되므로, 예측 칸을 비운 채 실행하지 않습니다.

### 결과 요약

14항목 중 **10 적중 · 3 빗나감 · 1 미실시**. 빗나간 셋이 전부 **설계된 교정 지점**이었습니다.

| # | 예측 | 결과 | 겨눈 곳 |
|---|---|---|---|
| D-1 | 404 | ✅ | |
| D-2 ① | 라이브러리 넣으면 200 | ❌ | **관문이 둘**임을 확인 |
| D-2 ② | prometheus 200 / metrics 유보 | ✅ | 두 층 독립 |
| D-3 | 404 · DOWN · `up`=0 | ✅ | |
| A-1 | 다른 포트, 9090 에 모임 | ✅ | |
| A-2 | Alertmanager = 룰 매니저 | ❌ | **§2 오답 교정** |
| A-3 | 같은 것을 보여줌 | ✅ | |
| A-4 | Prometheus 겹침 · 메트릭 역할 분담 | ✅ | **§1 자력 교정** |
| C-1 | 하루 1회 · 나머지 0 · 정상인데 0 | ✅ | |
| C-2 | 값 나옴 · 1234 · `job=node-exporter` | ✅ | 라벨 의문 제기 ⭐ |
| C-3 | CPU 정상 · 1234 생존 · HTTP 정상 | ✅ | **오답 교정** |
| C-4 | `up`=1 · HTTP 성공만 봄 | ✅ | **오답 교정 완결** |
| C-5 | Pushgateway 값 유지 | ❌ | ⭐ **미해결 물음 해소** |
| B-1 | firing 유지 · 메일 안 옴 | ✅ | 방향은 push 로 정정 |
| B-2 | 2덩어리 · 2통 | ❌ | 상자 ≠ 줄 |
| B-3 | — | ⏭️ | 05-01 |

**§3 병목**은 실습 중 질문에서 **"사람"** 으로 자력 교정됐습니다(인출 때는 "기계").

---

## 사전 상태 (실측)

| 항목 | 상태 |
|---|---|
| LGTM 스택 | `profiles: ["observability"]` 로 선언, 이 실습 중 **무변경** |
| `infra/alloy/config.alloy` | 빈 TODO 스텁 — 3-2 주차 산출물이므로 **채우지 않음** |
| `micrometer-registry-prometheus` | 없음 (D-1 전제) |
| `/actuator/prometheus` | 미노출 — `include: health,info,circuitbreakers` |
| 앱 | 호스트 `bootRun` → 8092 (컨테이너 아님) |

경계: 이 실습은 ROADMAP 3-2 주차(Actuator·Micrometer·OTel, Alloy)를 앞당기지 않습니다. Prometheus 는 별도 profile 로만 뜨고, 내릴 때 LGTM 은 영향받지 않습니다.

---

## D. Spring 연동 3관문

### D-1 — 의존성 없이 `/actuator/prometheus` → 404

```bash
curl -s -o /dev/null -w "%{http_code}\n" localhost:8092/actuator/prometheus
```

- **예측**: 404. 노출시켜야 한다고 배웠는데 노출시키지 않았으므로. ✅ **적중**
- **실제**:

  | 엔드포인트 | 응답 |
  |---|---|
  | `/actuator/health` · `/actuator/info` · `/actuator/circuitbreakers` | 200 |
  | `/actuator/prometheus` | **404** |
  | `/actuator/metrics` | **404** |

  200인 셋이 `include: health,info,circuitbreakers` 와 정확히 일치합니다.

- **해석**: 예측대로였지만, 묻지 않았던 `/actuator/metrics` 가 같이 404로 나온 것이 뜻밖의 소득이었습니다. **같은 404인데 이유가 다릅니다.**

  - `/actuator/metrics` — 빈은 **있습니다**(Actuator 스타터 기본 제공). `include` 에 없어서 안 열립니다.
  - `/actuator/prometheus` — 빈이 **없습니다**(`micrometer-registry-prometheus` 가 클래스패스에 없어 자동설정이 건너뜀). 노출도 안 됩니다.

  화면상 같은 숫자라 구분이 안 되므로, D-2 에서 라이브러리를 넣어 갈라 봅니다.

### D-2 — 두 관문을 하나씩 연다

관문이 둘이라는 것이 요점입니다. 의존성만으로도, `include` 만으로도 열리지 않습니다.

```bash
# 관문 ① build.gradle 의 micrometer-registry-prometheus 주석 해제 → 재기동 후
curl -s -o /dev/null -w "%{http_code}\n" localhost:8092/actuator/prometheus
# 관문 ② application.yml 의 include 에 prometheus 추가 → 재기동 후
curl -s localhost:8092/actuator/prometheus | head -5
```

- **예측** (①만 열었을 때): "이제는 되지 않을까" → 200 기대. ❌ **빗나감**
- **예측** (②까지 열었을 때): prometheus 는 200, metrics 는 "될지 모르겠음"(유보). ✅ **적중**
- **실제**:

  | 시점 | `/actuator/prometheus` | `/actuator/metrics` |
  |---|---|---|
  | D-1 (라이브러리 없음) | 404 | 404 |
  | 관문① (라이브러리만) | **404** | 404 |
  | 관문② (`include` 추가) | **200** | **404** |

  관문② 응답 상세: `Content-Type: text/plain;version=0.0.4;charset=utf-8`, `# TYPE` 계열 10개.
  기동 직후라 `application_ready_time_seconds 4.203`, 캐시 eviction 0 등 초기값뿐입니다.

- **해석**: 관문①에서 예측이 빗나간 것이 이 실습의 소득입니다. 라이브러리를 넣었는데도 404였습니다 — **빈이 생긴 것과 URL 이 열리는 것은 별개**이기 때문입니다.

  `prometheus` 는 D-1 의 "빈도 없고 노출도 없음"에서 관문① 이후 "빈은 있는데 노출이 없음"으로 **한 칸 이동**했습니다. 화면상 404로 같아 보이지만 안에서는 달라졌고, 그래서 `include` 한 줄로 열렸습니다.

  `metrics` 가 끝까지 404로 남은 것이 이 구조의 대조군입니다. `include` 에 `prometheus` 만 적었으므로 `prometheus` 만 열렸습니다. 두 엔드포인트가 같은 데이터를 보면서도 **창구가 달라** 각각 열어야 합니다(`metrics` 는 JSON 으로 이름을 훑는 창구, `prometheus` 는 노출 형식 텍스트로 한꺼번에 뱉는 창구).

  **빈 등록이 갈리는 지점** — Spring Boot 자동설정이 `@ConditionalOnClass(PrometheusMeterRegistry.class)` 로 클래스패스를 봅니다. 라이브러리가 없으면 조건이 거짓이라 자동설정 전체가 건너뛰어지고 빈이 안 만들어집니다. 그 뒤 `@ConditionalOnAvailableEndpoint` 가 `include` 목록을 봅니다. **조건 평가가 두 번** 일어나는 셈입니다.

  곁가지: `include` 는 화이트리스트입니다. `"*"` 로 전부 열 수 있지만 `/actuator/env`(환경변수 — DB 비밀번호), `/actuator/heapdump`(힙 덤프 — 메모리에 있던 토큰·개인정보)가 함께 열립니다. 필요한 것만 명시하는 편이 안전합니다.

### D-3 — `metrics_path` 를 안 주면 `/metrics` 를 긁어 실패

`prometheus.yml` 에 `notification-service-wrong-path` job 을 미리 넣어 두었습니다.

```bash
open http://localhost:9090/targets   # 두 job 을 나란히 대비
curl -s -o /dev/null -w "%{http_code}\n" localhost:8092/metrics
```

- **예측**: (1) `/metrics` → 404, (2) Targets 에서 DOWN, (3) `up` = 0. ✅ **셋 다 적중**

  덧붙인 예측: "metrics_path 를 안 주면 어디서 정보를 얻을지 몰라 아무것도 수집 안 한다" → **절반 적중**(수집 안 되는 건 맞지만 이유가 다름, 아래 해석 참고)

- **실제**:

  | job | scrapeUrl | health | lastError |
  |---|---|---|---|
  | `notification-service` | `.../actuator/prometheus` | **up** | (없음) |
  | `notification-service-wrong-path` | `.../metrics` | **down** | `server returned HTTP status 404 Not Found` |

  `up` 전체 — node-exporter 1 · notification-service 1 · prometheus 1 · pushgateway 1 · **wrong-path 0**

- **해석**: 같은 주소(`host.docker.internal:8092`)를 긁는 두 job 이 **경로 하나로** 갈렸습니다.

  예측에서 "어디서 얻을지 몰라서"라고 한 부분이 실제와 다릅니다. Prometheus 는 **모르는 게 아니라 기본값 `/metrics` 를 씁니다.** 설정이 빠진 것을 눈치채지 못하고 조용히 엉뚱한 경로로 가서, 앱이 404를 돌려주고 나서야 실패로 기록됩니다. 설정 오류가 **런타임 404 로 뒤늦게** 드러나는 셈이라, 멈춰서 알려 주는 것보다 고약합니다.

  ⚠️ **`up` = 0 의 이유는 아직 확정하지 않습니다.** 여기서는 404 라서 0 이 나왔지만, `up` 이 0 이 되는 조건이 "404 여서"인지 더 넓은 무언가인지는 이 실험만으로 안 갈립니다. **C-4 에서 "응답은 200 인데 내용이 깨진" 경우**를 만들어 다시 묻습니다.

### D 묶음 정리

같은 404 가 **세 번** 나왔는데 이유가 셋 다 달랐습니다.

| 시점 | 404 의 이유 |
|---|---|
| D-1 | 빈이 없음 (라이브러리 미포함 → 자동설정 건너뜀) |
| D-2 관문① | 빈은 있는데 노출 안 됨 (`include` 누락) |
| D-3 | 엔드포인트는 열렸는데 **긁는 쪽이 다른 경로**를 봄 |

§Spring 관점의 "기본으로는 열려 있지 않아 명시적으로 노출해야 한다"가 실제로는 이 세 층입니다.

**D 는 "관이 뚫렸는가"까지입니다.** 무엇을 수집할지(counter·gauge·histogram 선택, JVM 힙·HikariCP·consumer lag 계측)는 이 실습 범위 밖이고, ROADMAP **3-2 주차(Actuator·Micrometer·OTel)** 의 몫입니다. 볼 대상 목록은 이미 [관측 시나리오와 운영 절차](../docs/02-scenarios-and-operations.md)에 있고, 아직 붙이지 않았을 뿐입니다.

> 📌 D-1 이전까지 한 것은 `curl` 로 앱에 **직접 물어본 것**뿐이라, 값은 뱉는 순간 사라졌습니다. 스택을 띄운 지금에야 **긁는 쪽(scrape)과 쌓는 쪽(TSDB)** 이 생겨 시계열로 남습니다. 메트릭을 **내는 쪽(앱)** 과 **모으는 쪽(Prometheus)** 이 다른 프로세스라는 것이 A 묶음의 출발점입니다.

---

## A. 스택 4구성요소 + 내부 4부품

### A-1 — 프로세스 4개가 각각 뜬다

```bash
docker compose -f infra/compose.yaml --profile prometheus up -d
docker compose -f infra/compose.yaml --profile prometheus ps
```

- **예측**: 각각의 서비스는 다른 포트로 떠 있고, Prometheus UI 에서 한 번에 봐서 9090. ✅ **적중** (단 "다른 네트워크"는 아님 — 같은 docker 네트워크, 다른 것은 **프로세스**)
- **실제**:

  | 컨테이너 | 포트 |
  |---|---|
  | `nlab-prometheus` | 9090 |
  | `nlab-node-exporter` | 9100 |
  | `nlab-alertmanager` | 9093 |
  | `nlab-pushgateway` | 9091 |

- **해석**: 넷이 각각 다른 포트에 있습니다. 프로세스가 넷이라는 뜻입니다.

### A-2 — 내부 4부품을 화면으로 구분

네 화면이 **한 프로세스 안의 네 부품**입니다. A-1 의 네 컨테이너와 층이 다릅니다.

| 부품 | 화면 |
|---|---|
| 스크레이프 매니저 | http://localhost:9090/targets |
| TSDB | http://localhost:9090/tsdb-status |
| 룰 매니저 | http://localhost:9090/rules |
| 웹 UI / API | http://localhost:9090/graph |

- **예측**: "Alertmanager 는 룰 매니저, exporter 는 스크레이프 매니저로 보인다." ❌ **빗나감** — 이름이 닮아 짝지었으나 짝이 아니라 **층이 다름**
- **실제**:

  | 경로 | 응답 |
  |---|---|
  | `:9090/targets` · `/tsdb-status` · `/rules` · `/graph` | 전부 **200** |
  | `:9090/alertmanager` · `/node-exporter` · `/exporters` | 전부 **404** |

  `docker exec nlab-prometheus ps` → **PID 1 에 프로세스 하나뿐**. 네 화면이 전부 이 하나에서 나옵니다.

- **해석**: `:9090/alertmanager` 가 404 인 것이 결정적입니다. Alertmanager 가 룰 매니저였다면 `:9090` 아래 어딘가에 있어야 하는데 없고, **9093 에 따로** 있습니다. 반면 `/rules` 는 200 이라 룰 매니저는 Prometheus **안**에 있습니다. 둘은 같은 것일 수 없습니다.

  짝이 아니라 **일하는 쪽과 상대하는 쪽**입니다.

  | 안 (부품) | 밖 (별개 프로세스) | 관계 |
  |---|---|---|
  | 룰 매니저 | Alertmanager | 룰이 **판정**하고 → Alertmanager 가 **전달** |
  | 스크레이프 매니저 | node-exporter | 스크레이프가 **긁고** → exporter 가 **긁힘** |

  - **룰 매니저**: 주기적으로 TSDB 에 쿼리를 날려 조건을 평가하고, 충족되면 알림을 firing 으로 표시합니다. 여기까지가 Prometheus 안의 일입니다.
  - **Alertmanager**: firing 알림을 HTTP 로 받아 그룹핑·억제·silence 를 거쳐 사람에게 보냅니다. **판단과 전달이 갈립니다** — Prometheus 는 조건이 참인지만 알고, 누구에게 몇 번 보낼지는 모릅니다. (B-1 에서 실측)
  - **node-exporter**: Prometheus 는 리눅스 커널에게 CPU 사용률을 물을 방법이 없습니다. `/proc` 을 읽어 Prometheus 가 아는 텍스트 형식으로 바꿔 `:9100/metrics` 에 걸어 두는 **번역기**입니다. Prometheus 는 HTTP GET 만 할 줄 알면 됩니다.

  **핵심: 부품은 프로세스가 아닙니다.** 네 부품은 PID 1 하나 안에서 역할을 나눈 것이고, Alertmanager·exporter 는 아예 다른 프로그램입니다. §2 에서 층이 밖으로 나간 지점이 여기입니다.

### A-3 — Grafana 가 :9090 REST API 를 데이터소스로 쓴다

```bash
curl -s -G localhost:9090/api/v1/query --data-urlencode query=up
curl -s -o /dev/null -w "%{content_type} %{size_download}\n" localhost:9090/graph
curl -s localhost:9090/graph | grep -c "node-exporter"
```

- **예측**: (1) 동일한 것을 보여준다(스크레이핑 결과) ✅ **적중** · (2) Grafana 는 API 를 쓴다, 경로는 모름 ✅ **적중** · (3) "UI 가 부품"의 뜻 — 모르겠음
- **실제**:

  | | Content-Type | 크기 | `node-exporter` 문자열 |
  |---|---|---|---|
  | `/graph` | `text/html` | 734 bytes | **0건** |
  | `/api/v1/query` | `application/json` | 650 bytes | 값 5건 |

- **해석**: `/graph` 는 734바이트 HTML 이고 **데이터가 한 글자도 없습니다**. 브라우저에서 표가 뜨는 것은 그 안의 자바스크립트가 `/api/v1/query` 를 호출해 받아 그린 것입니다. 손으로 친 curl, 브라우저 UI, Grafana 가 **전부 같은 창구**를 씁니다(구간 조회는 `/api/v1/query_range`).

  **(3)의 답 — "UI 가 부품"은 화면을 그리는 일이 아닙니다.** 화면은 브라우저가 그립니다. UI 부품이 맡은 것은 **TSDB 에 쌓인 시계열을 밖에서 물어볼 수 있게 창구를 여는 일**입니다 — PromQL 을 받아 TSDB 에서 꺼내 계산해 돌려줍니다. 이 창구가 없으면 데이터가 아무리 쌓여도 꺼낼 방법이 없어 Grafana 도 못 붙습니다.

  **TSDB 가 중심인 이유**가 여기서 드러납니다. 나머지 셋이 전부 TSDB 를 상대하고, 방향만 다릅니다.

  ```
  스크레이프 매니저 ──쓴다──▶ TSDB ◀──읽는다── 웹 UI / API
                              ▲
                              │ 읽고 쓴다
                          룰 매니저
  ```

  들어오는 문이 하나(스크레이프 — 바깥에서 들여옴), 나가는 문이 하나(UI/API), 안에서 되새기는 것이 하나(룰)입니다.

### A-4 ⭐ — 두 스택 대비 (§1 오답 정면 교정)

두 profile 을 동시에 켜서, 축이 다른 두 묶음이 한 클러스터에 공존하는 것을 봅니다.

```bash
docker compose -f infra/compose.yaml --profile prometheus --profile observability up -d
docker compose -f infra/compose.yaml --profile prometheus --profile observability ps
```

- **예측**: "지금 너무 많이 올라가서 모르겠는데, 아마 **프로메테우스가 겹치며** 하나는 **메트릭을 두고 역할을 나눈** 4계층이고 나머지는 LGTM 스택일 것" ✅ **2번·3번 적중**
- **실제**: 13개가 세 묶음으로 갈립니다.

  | 묶음 | 서비스 | 나누는 기준 |
  |---|---|---|
  | ① Prometheus 스택 | prometheus · node-exporter · alertmanager · pushgateway | **메트릭 한 축** 안의 역할 분담 |
  | ② LGTM 스택 | loki · mimir · tempo · alloy · grafana | **신호 종류별** 묶음 |
  | ③ 앱 인프라 (profile 없음) | kafka · postgres · redis · opensearch · wiremock · mailhog · kafka-ui | 관측과 무관 |

  "너무 많아서 모르겠다"고 한 것은 **13개 중 7개가 ③ 노이즈**였기 때문입니다. profile 없이 항상 뜨는 서비스입니다.

- **해석**: **§1 오답의 정체가 여기서 드러납니다.**

  ①의 넷은 **전부 메트릭만** 다룹니다 — Prometheus 가 긁고, node-exporter 가 OS 값을 번역하고, pushgateway 가 단명 작업 값을 받아 두고, alertmanager 가 결과를 사람에게 보냅니다. **하나의 신호를 두고 공정을 나눈 것**입니다.

  ②는 축이 다릅니다 — Loki(로그) · Mimir(메트릭) · Tempo(트레이스) · Alloy(수집 에이전트) · Grafana(통합 조회). **신호 종류로 나눈 묶음**입니다.

  > "Prometheus 스택 4구성요소"를 물었는데 LGTM 으로 답한 것은, **가로로 자른 것을 물었는데 세로로 자른 것을 답한 셈**입니다. 둘 다 "관측 스택"이라 불리지만 자르는 방향이 다릅니다.

  **겹치는 하나는 메트릭**입니다. ①의 Prometheus 와 ②의 Mimir 가 같은 자리입니다 — 둘 다 메트릭 TSDB 입니다. 그래서 ②에는 Loki·Tempo 가 있지만 ①에는 로그·트레이스 담당이 없습니다.

  ```
          로그        메트릭       트레이스
  LGTM:   Loki        Mimir        Tempo
                        ↕ 같은 자리
  Prom:              Prometheus
                     (+ exporter, pushgateway, alertmanager)
  ```

  실무에서 둘을 함께 쓰기도 합니다 — Prometheus 가 긁어 **remote write 로 Mimir 에 넘기면** 수집과 장기 저장이 나뉩니다. 상세는 09-01·09-02 가 SSOT 입니다.

  > ⚠️ **사후 발견 (정직하게 기록)**: 정리 단계에서 확인해 보니 **Loki·Mimir·Tempo 는 A-4 시점에 이미 기동 실패로 죽어 있었습니다**(`Exited (1)`·`Exited (2)`). 설정 파일이 `config.alloy` 와 마찬가지로 **주석만 있는 빈 스텁**(실질 0줄)이라 파싱 단계에서 죽습니다 — Mimir `Error parsing config file: EOF`, Tempo `unknown backend`, Loki `Config.Validate()` 패닉. ROADMAP 3단계 미착수 상태이므로 당연한 결과입니다.
  >
  > 진행 중에는 `docker compose ps` 의 컨테이너 **목록만 보고 STATUS 열을 안 읽어** 이를 놓쳤습니다. 실제로 살아 있던 LGTM 컨테이너는 **Grafana·Alloy 둘뿐**입니다.
  >
  > **위 결론(두 묶음의 축이 다르다 · 겹치는 것은 메트릭)은 compose 선언 기준이라 그대로 유효합니다.** 다만 "두 스택이 실물로 나란히 떠 있는 것을 보았다"는 아닙니다. 실물 대비는 LGTM 설정이 채워지는 **3-2 주차**에 다시 해야 합니다.
  >
  > 교훈: `up -d` 직후 목록만 보고 "떴다"고 판단하지 말 것. **`STATUS` 열 또는 종료 코드를 확인**해야 합니다(0 이 아니면 실패).

### A 묶음 정리

§1·§2 오답의 뿌리가 **하나의 층 혼동**이었음이 실측으로 갈렸습니다.

| 물음 | 오답 | 실측이 보여준 것 |
|---|---|---|
| §2 내부 4부품 | Alertmanager · exporter | 둘 다 `:9090` 아래 **404**, 별도 포트에 존재. 부품 넷은 PID 1 하나 안 |
| §1 스택 4구성요소 | LGTM | 축이 다름 — 스택은 **메트릭 한 축의 역할 분담**, LGTM 은 **신호 종류별 묶음** |

두 오답 모두 **"이것은 어느 층의 이야기인가"** 를 세우지 못한 데서 나왔고, 층을 가르는 기준은 결국 **프로세스 경계**였습니다. `docker compose ps` 와 `:9090` 화면을 나란히 놓으면 갈립니다.

---

## C. 단명 프로세스·textfile collector

### C-1 — 배치를 직접 scrape 하면 대부분 헛친다

`HistoryArchiveScheduler` 는 `0 0 3 * * *` — 하루 한 번, 순식간에 끝납니다. 15s 주기 scrape 가 이 순간을 잡을 확률을 생각해 봅니다.

- **예측**: 하루 1번 성공, 시간이 안 맞으면 0번 · `up` 은 나머지 시간 모두 0 · **배치가 정상 동작해서 내려가도 스크레이프 실패로 0 이 다 찍힌다** ✅ **적중**
- **실제**: 15초 눈금 × 하루 5,760회. `archive-cron = 0 0 3 * * *` 은 하루 한 번, 몇 초 만에 끝납니다. 40초 걸려도 명중 2~3회, 5초면 눈금 사이에 통째로 들어가 **명중 0회**도 가능합니다.
- **해석**: 예측대로지만 문제가 "0 이 찍힌다"로 끝나지 않습니다. 두 겹이 더 있습니다.

  1. **최종 결과를 영영 못 봅니다.** 처리 건수는 배치가 끝나기 **직전**에 확정되는데, 그 순간이 눈금 사이면 "몇 건 처리했나"가 그래프에 한 번도 안 남습니다. 배치는 성공했는데 성공했다는 증거가 없습니다.
  2. **`up=0` 은 실패와 구분되지 않습니다.** 정상 종료해서 0 인지, 죽어서 0 인지, 애초에 안 떴는지 — 그래프만 보면 셋이 똑같이 생겼습니다.

  긁는 방식은 **오래 사는 프로세스**를 전제합니다. 그 전제가 깨지는 곳이 단명 작업입니다.

### C-2 — `.prom` 파일 → node-exporter 가 실어 보낸다

실재하는 배치(UC-5 아카이빙)가 남긴 값을 흉내 냅니다.

```bash
cat > infra/node-exporter/textfile/archive.prom <<'EOF'
# HELP notification_archive_last_success_timestamp_seconds 마지막 아카이빙 성공 시각
# TYPE notification_archive_last_success_timestamp_seconds gauge
notification_archive_last_success_timestamp_seconds 1753660800
# HELP notification_archive_rows_total 아카이빙한 행 수
# TYPE notification_archive_rows_total gauge
notification_archive_rows_total 1234
EOF
curl -s localhost:9100/metrics | grep notification_archive
```

- **예측**: (1) 값이 나온다, 원리는 모름 — "자원 내용도 파일로 기록해서 던지기 때문 아닐까" **절반 적중** · (2) 1234 가 나온다 ✅ · (3) `job="node-exporter"`, `instance="node-exporter:9100"` ✅ **적중** ("라벨이 어디 있지?" 라는 의문 포함)
- **실제**: `:9100/metrics` 전체 1,916줄 안에 섞여 나옵니다.

  ```
  notification_archive_last_success_timestamp_seconds 1.7536608e+09
  notification_archive_rows_total 1234
  ```

  Prometheus 조회 시 라벨 — `{instance="node-exporter:9100", job="node-exporter"}`

- **해석**: "파일로 기록해서 던진다"는 추측이 **절반 맞습니다.** node-exporter 가 CPU 를 읽는 방법이 실제로 파일 읽기입니다 — 리눅스는 `/proc/stat`·`/proc/meminfo` 같은 **파일**로 커널 상태를 노출하고, node-exporter 는 그것을 텍스트로 바꾸는 프로그램입니다.

  textfile collector 는 거기에 하나를 더 얹은 것입니다. compose 의 이 옵션이 켜 준 것이지 기본 동작이 아닙니다:

  ```yaml
  --collector.textfile.directory=/var/lib/node_exporter/textfile
  ```

  `.prom` 파일 형식이 `# HELP` / `# TYPE` / `이름 값` 인 이유도 여기 있습니다 — node-exporter 가 자기 `/metrics` 에 내는 **형식과 같아서** 읽어 그대로 이어 붙이면 됩니다. 변환이 필요 없습니다.

  **⭐ "라벨이 어디 있지?" — 이 의문이 C-5 의 복선입니다.**

  `.prom` 파일에는 라벨이 **한 글자도 없습니다**(`notification_archive_rows_total 1234`). 라벨을 붙인 것은 **Prometheus** 입니다. 긁을 때 "어느 job 의 어느 instance 에서 왔나"를 자기 설정대로 적어 넣습니다.

  ```yaml
  - job_name: "node-exporter"
    static_configs:
      - targets: ["node-exporter:9100"]
  ```

  **긁은 자리가 곧 정체가 됩니다.** 그래서 뒤틀림이 생깁니다 — 이 값은 **아카이빙 배치**가 만들었는데 라벨은 `job="node-exporter"` 입니다. 배치의 정체가 사라지고 **파일을 대신 들고 있던 쪽의 이름**이 붙었습니다. 배치가 100대에서 돌면 `instance` 로는 갈리지만 `job` 은 전부 `node-exporter` 입니다.

  배치로 묶어 보려면 파일 안에 직접 써야 합니다: `notification_archive_rows_total{job="history-archive"} 1234`

### C-3 ⭐ — 파일을 깨뜨려도 노드 메트릭은 정상 (오답 직접 교정)

```bash
echo 'broken{' > infra/node-exporter/textfile/bad.prom
curl -s localhost:9100/metrics | grep -E "node_textfile_scrape_error|node_cpu_seconds_total" | head
```

인출에서 **"정상적인 파일이 아니라면 0이라서 실패"** 라고 답했던 지점입니다.

- **예측**: (1) CPU 메트릭 나온다 ✅ · (2) `scrape_error` 값 모름 · (3) 멀쩡한 파일 값 그대로 있음 ✅ · (4) HTTP 는 멀쩡히 수행되는 것처럼 보임 ✅ — **셋 적중, 하나 미상**
- **실제**: `bad.prom`(`broken{`) 을 넣은 상태에서

  | 확인 | 결과 |
  |---|---|
  | HTTP 응답 | **200**, 104,868 bytes |
  | `node_cpu_seconds_total` | **32건 정상** |
  | `notification_archive_rows_total` | **1234 그대로** |
  | `node_textfile_scrape_error` | **1** ← (2)의 답 |
  | `node_textfile_mtime_seconds` | `archive.prom` **1건만** (`bad.prom` 빠짐) |
  | `node_scrape_collector_success{collector="textfile"}` | **1** |

- **해석**: **오답이 실측으로 뒤집혔습니다.** 깨진 파일 하나가 나머지를 전혀 오염시키지 못했습니다.

  읽어 낼 것은 읽고 못 읽은 것만 표시했습니다 — `mtime` 목록에서 `bad.prom` 만 빠졌고, `scrape_error 1` 로 신고했으며, 그러면서도 `collector_success` 는 **1**(성공)입니다. `scrape_error` 는 **실패를 알리는 별도 메트릭**이지 다른 값을 죽이는 스위치가 아닙니다. 문제를 숨기지도, 전파하지도 않습니다.

  반대를 상상하면 이 설계가 왜 맞는지 드러납니다 — 파일 하나 잘못 썼다고 CPU·메모리·디스크가 전부 사라지면 **배치 스크립트의 오타가 서버 모니터링을 통째로 날립니다.**

  (3)이 특히 중요합니다. 깨진 파일과 멀쩡한 파일이 **같은 디렉토리**에 있는데도 서로 영향을 안 줍니다. **파일 단위로 격리**됩니다.

### C-4 ⭐ — `up` 은 HTTP 성공만 본다

```bash
curl -s 'localhost:9090/api/v1/query?query=up{job="node-exporter"}'
```

- **예측**: (1) 1 이다 · (2) "up 은 호출 결과가 200 인지 확인하는 건데 지금은 200 이 나옴" · (3) 차이는 "HTTP 요청 성공/실패 여부" — ✅ **셋 다 적중**
- **실제**: 같은 시점 TSDB 에 두 값이 **나란히** 있습니다.

  ```
  up{job="node-exporter"}                   = 1     ← 파일이 깨졌는데도
  up{job="notification-service-wrong-path"} = 0     ← 404
  node_textfile_scrape_error{job="node-exporter"} = 1
  ```

- **해석**: `up=1` 과 `scrape_error=1` 이 **동시에** 참인 것이 오답의 반증입니다. "파일이 깨졌으니 실패"라면 둘이 같은 방향이어야 하는데 반대입니다. 응답은 성공했고, 그 응답 **안에** 문제가 있다는 신고가 담겨 있습니다.

  정확히는 `up` 은 HTTP 200 이면서 **본문이 파싱 가능한 형식**이면 1 입니다. 연결 거부·타임아웃·404·형식이 아예 깨져 못 읽는 경우가 0 입니다. `bad.prom` 은 이 기준을 통과했습니다 — node-exporter 가 **자기 선에서 걸러 내고** 200 과 유효한 텍스트를 돌려줬으니, Prometheus 입장에서는 흠 없는 응답입니다.

  | | HTTP | `up` | 문제는 어디에 |
  |---|---|---|---|
  | D-3 wrong-path | **404** | **0** | 응답을 못 받음 |
  | C-3 깨진 파일 | **200** | **1** | 응답 **안의** 내용 일부 |

  > **`up` 은 문을 두드려 대답이 오는지만 봅니다. 안에서 무슨 일이 벌어지는지는 안 봅니다.**

  **운영 함의**: `up` 만 걸어 두면 이 상황을 영영 못 봅니다. 대시보드는 전부 초록인데 배치 메트릭만 조용히 낡아 갑니다. 그래서 `node_textfile_scrape_error > 0` 같은 **내용 쪽 지표**를 따로 겁니다. `node_textfile_mtime_seconds` 도 같은 용도로, "6시간 넘게 안 갱신됐다"를 잡아 **배치가 죽었는데 파일만 남은** 경우를 찾아냅니다.

  ⭐ 파일은 **낡아도 남아 있습니다.** 그 "남아 있음"이 C-5 의 주제입니다.

### C-5 ⭐⭐ — textfile vs Pushgateway (미해결 물음의 답)

> "파일에 데이터를 담아 보내는 방식이 결국 push 방식과 다를 게 있는가?"

값을 밀어 넣고, **배치를 다시 안 돌린 채** 시간이 지나도 남는지 봅니다.

```bash
# 1) Pushgateway 에 배치 결과를 push
echo 'notification_archive_rows_total 5678' | \
  curl --data-binary @- http://localhost:9091/metrics/job/history-archive

# 2) 배치를 다시 돌리지 않고 확인
curl -s localhost:9091/metrics | grep notification_archive_rows_total
curl -s 'localhost:9090/api/v1/query?query=notification_archive_rows_total'

# 3) textfile 쪽과 대비 — 파일을 지우면?
rm infra/node-exporter/textfile/archive.prom
curl -s localhost:9100/metrics | grep notification_archive || echo "(사라짐)"
```

- **예측**: (1) "5678 이 나왔는데 1234 가 나올 수 있는 건가요?" · (2) `instance=""`, `job="history-archive"` ✅ · (3) **"Pushgateway 를 재시작하더라도 이미 밀어넣은 거라 그대로 아닌가"** ❌ **빗나감** / `archive.prom` 을 지우면 앞으로는 1234 없음 ✅ · (4) "exporter 파일을 통한 것도 일종의 push 같다. 장단점 차이는 아직 모르겠음" — **관찰은 정확, 축은 미확정**
- **실제**:

  **③ 조회 — 둘 다 나옵니다.** 이름이 같아도 라벨이 다르면 **별개의 시계열**입니다((1)의 답).

  ```
  {job="node-exporter", instance="node-exporter:9100"}  1234   ← textfile
  {job="history-archive", instance=""}                  5678   ← push
  ```

  **⭐ 대칭 실험 — 같은 조작(컨테이너 재시작)에 정반대로 반응했습니다.**

  | | 재시작 전 | 재시작 후 |
  |---|---|---|
  | **Pushgateway** (5678) | 있음 (136줄) | **❌ 사라짐** (111줄) |
  | **node-exporter** (1234) | 있음 | **✅ 그대로** |

  **TSDB 쪽 시계열 — 과거는 남고 미래가 끊깁니다.**

  ```
  job=history-archive : 점 15개 · 마지막 15:08:12 = 5678  ❌ 끊김
  job=node-exporter   : 점 31개 · 마지막 15:09:12 = 1234  ✅ 계속 들어옴
  ```

  > ⚠️ **측정 함정 (기록해 둡니다)**: 재시작 직후 곧바로 조회하면 컨테이너가 아직 뜨는 중이라 빈 응답이 옵니다. 이것을 "값이 사라졌다"로 읽으면 오판입니다. `RestartCount`·`StartedAt` 으로 재시작이 실제로 일어났는지 확인하고, **HTTP 200 이 돌아온 뒤에** 조회해야 합니다. 이 세션에서 실제로 한 번 오판했다가 `StartedAt` 대조로 잡았습니다.

- **해석**: **⭐ 미해결 물음의 답입니다.**

  > "파일에 데이터를 담아 보내는 방식이 결국 push 방식과 다를 게 있는가?"
  >
  > **배치 입장에서는 같습니다. 값의 운명에서는 다릅니다.**

  "exporter 파일을 통한 것도 일종의 push 같다"는 관찰이 맞습니다 — 배치는 둘 다 "값을 어딘가 놓고 죽을" 뿐입니다. 갈리는 것은 **놓은 자리**입니다.

  ```
  textfile:  배치 → [디스크 파일] → node-exporter 가 읽음 → Prometheus 가 긁음
  push:      배치 → [Pushgateway 메모리] ──────────────→ Prometheus 가 긁음
  ```

  **축 ① 값을 누가 들고 있나 — 디스크냐 메모리냐.** 재시작 실험이 이것을 갈랐습니다. node-exporter 는 죽었다 살아나도 1234 가 나옵니다. 값이 node-exporter 안이 아니라 **디스크의 `archive.prom`** 에 있어서, 새로 뜨면 다시 읽기 때문입니다. Pushgateway 의 5678 은 **자기 메모리 안**에 있었고 프로세스와 운명을 같이했습니다. 배치는 이미 끝나서 다시 보내 줄 사람도 없으니 **영영 사라집니다.**

  **"Pushgateway 에서 사라지는 거지 Prometheus 는 들고 있는 것 아닌가?"** — 이 되물음이 절반 맞습니다. **과거는 남고 미래가 끊깁니다.**

  Prometheus 의 저장 방식은 **긁을 때마다 그 순간의 값을 도장 찍는 것**입니다.

  ```
  15:07:42  Pushgateway 긁음 → 5678 있음 → 도장 (5678)
  15:07:57  긁음 → 5678 있음 → 도장 (5678)
  15:08:12  긁음 → 5678 있음 → 도장 (5678)
            ─── 재시작, 메모리 비워짐 ───
  15:08:27  긁음 → 빈손 → 찍을 게 없음
  15:08:42  긁음 → 빈손 → 찍을 게 없음
  ```

  **이미 찍힌 도장은 안 지워집니다.** 그래서 과거 15개가 TSDB 에 남습니다. 하지만 **새로 찍을 값이 없어** 선이 끊깁니다. node-exporter 는 재시작해도 디스크의 `archive.prom` 을 다시 읽으니 선이 안 끊깁니다.

  **"과거가 남는데 뭐가 문제냐"** — 배치가 **하루 한 번** 도는데 Pushgateway 가 죽으면 다음 배치까지 **24시간 동안 값이 없습니다.** 그 사이 대시보드는 빈칸이고 "마지막 아카이빙이 언제였나"에 답이 안 나옵니다. 배치를 다시 돌릴 수도 없습니다 — 어제 데이터는 이미 처리됐습니다. textfile 은 같은 상황에서도 파일이 디스크에 남아 계속 답합니다.

  **축 ② 누가 정체를 정하나** — C-2 의 "라벨이 어디 있지?" 가 여기로 이어집니다.

  | | job 라벨 | 정한 주체 |
  |---|---|---|
  | textfile | `node-exporter` | **Prometheus** 가 긁은 자리를 보고 |
  | push | `history-archive` | **배치** 가 URL(`/metrics/job/history-archive`)에 박아서 |

  textfile 은 배치가 **자기 이름을 잃습니다.** 어느 노드인지는 남지만(`instance`) 무슨 작업인지는 사라집니다. push 는 배치가 자기를 소개합니다.

  **축 ③ 어느 노드에 매이나.** textfile 파일은 **특정 노드의 디스크**에 있어 "이 서버의 배치"라는 뜻이 자동으로 붙습니다. Pushgateway 는 노드와 무관해서, K8s Job 이 어느 노드에 스케줄되든 같은 곳으로 보냅니다.

  **그래서 언제 무엇을:**

  | | 맞는 경우 |
  |---|---|
  | **textfile** | 노드에 고정된 배치 — cron, 백업 스크립트, 디스크 점검. `HistoryArchiveScheduler` 처럼 특정 서버에서 도는 것 |
  | **Pushgateway** | 노드가 안 정해진 작업 — K8s Job, CI 파이프라인 |

  실무 권고는 **textfile 우선**입니다. Pushgateway 는 단일 장애점이 되고(실측대로 죽으면 다 날아감), 반대로 값이 **영영 남아** "이미 끝난 작업의 낡은 값"이 계속 조회되는 문제도 있습니다 — 명시적으로 지워야 사라집니다.

  > 본문의 **"textfile 은 push 를 노드 안으로 국소화한 것"** 이 이 뜻입니다. push 이긴 한데 목적지가 원격 서버가 아니라 **같은 노드의 디스크**입니다.

### C 묶음 정리

| 물음 | 오답/미해결 | 실측 |
|---|---|---|
| C-3 파일 깨짐 | "0 이라서 실패" | CPU 32건 정상 · 1234 생존 · `scrape_error 1` 만 · **파일 단위 격리** |
| C-4 `up` | (같은 오해) | `up=1` 과 `scrape_error=1` 이 **동시에 참** — `up` 은 문 두드림만 봄 |
| C-5 ⭐ 미해결 | "파일 쓰기와 push 가 다를 게 있는가" | **배치 입장에선 같고, 값의 수명·정체·노드 결속에서 다름** |

---

## B. Alertmanager 분리·그룹핑

> ⚠️ 존재 증명까지만 합니다. 그룹핑 타이머 3종·라우팅 트리 상세는 **05-01 이 SSOT** 입니다.

### B-1 — Prometheus 만으로는 알림이 안 간다

```bash
docker compose -f infra/compose.yaml --profile prometheus stop alertmanager
open http://localhost:9090/alerts     # firing 인데
open http://localhost:8025            # MailHog — 아무것도 안 옴
```

- **예측**: (1) `:9090/alerts` 는 그대로 있을 것 ✅ — 단 이유는 **"Alertmanager 가 긁어가는 형태"** ❌ (방향이 반대) · (2) Alertmanager 가 내려갔으면 메일 도착 안 함 ✅
- **실제**: Alertmanager 중지(`:9093` code=000) 후에도

  ```
  :9090/alerts — firing 2건 유지
     LabAlwaysFiring  instance=node-exporter:9100
     LabAlwaysFiring  instance=localhost:9090

  prometheus_notifications_errors_total  = 3
  prometheus_notifications_dropped_total = 6
  ```

  메일함은 2통뿐이고 **둘 다 중지 전 도착**(04:55·05:55)이었습니다. 중지는 06:11 이고, 이후 새 메일 **0통**입니다.

- **해석**: **"룰은 계속 돌고 발송만 끊겼다"** 가 숫자로 증명됐습니다. `dropped_total=6` 이 핵심입니다 — Prometheus 는 알림을 만들었고, 보내려 했고, **실패해서 버렸습니다.** 재시도로 쌓아 두지 않습니다.

  **⚠️ 방향 정정**: "Alertmanager 가 보고 본인이 긁어가는 형태"라고 예측했으나 **반대**입니다. Prometheus 가 Alertmanager 에게 **보냅니다(push)**. 설정이 증거입니다 — `scrape_configs` 가 아니라 `alerting` 블록입니다.

  ```yaml
  alerting:
    alertmanagers:
      - static_configs:
          - targets: ["alertmanager:9093"]
  ```

  Prometheus 가 "어디로 보낼지"를 알고, Alertmanager 설정에는 Prometheus 주소가 **아예 없습니다.** Prometheus 생태계가 거의 전부 pull 인데 **여기 한 곳만 push** 라 헷갈리기 쉬운 자리입니다. 알림은 터지는 즉시 가야 하기 때문입니다 — 긁는 방식이면 다음 주기까지 기다립니다.

  `dropped_total` 이 올라간다는 것 자체가 방향의 증거이기도 합니다. **보낸 쪽이 버린 것**이지, 받을 쪽이 안 가지러 온 것이 아닙니다.

  #### ⭐ §3 오답 교정 — 병목은 기계가 아니라 사람

  > **자기 답(교정 후)**: "사용자가 50개의 알람을 한번에 보는 게 문제입니다. 50개가 모두 동일 이슈이면 더더욱 그렇습니다." ✅

  인출 때 **"기계(Prometheus 가 힘들어서)"** 로 답했던 것에서 축이 옮겨졌습니다.

  기계는 안 힘듭니다. 알림 50건은 HTTP 요청 몇 개일 뿐이라 Prometheus 도 Alertmanager 도 아무렇지 않습니다. **힘든 것은 새벽 3시에 휴대폰을 받는 사람**입니다.

  - 스위치 1대 문제인지 서버 50대 문제인지 **판단이 안 섭니다**
  - 50통을 넘기는 동안 **진짜 다른 문제**가 섞여도 못 봅니다
  - 반복되면 **알림을 무시하게 됩니다** (alert fatigue)

  Alertmanager 의 도구 넷(그룹핑·억제·silence·라우팅)이 전부 **사람 쪽 문제**를 푸는 도구입니다. 기계 부하를 줄이는 것이 아닙니다. 그래서 별도 프로세스로 갈라 놓았고, **"판단은 Prometheus, 전달은 Alertmanager"** 라는 분리가 여기서 나옵니다.

### B-2 — 다중 알림 → 그룹핑으로 1건

```bash
docker compose -f infra/compose.yaml --profile prometheus start alertmanager
open http://localhost:9093            # instance 다른 2건이 어떻게 보이는가
open http://localhost:8025            # 메일은 몇 통인가
```

- **예측**: (1) "룰에 등록된 게 Prometheus·exporter 2개이며 **둘 다 라벨이 다르니 2덩어리**" ❌ **빗나감** · (2) 알람이 2개이니 메일도 2통 ❌ **빗나감**
- **실제**: **1덩어리**입니다. 응답 최상위 배열 길이가 1이고, 그 안에 알림 2건이 들어 있습니다.

  ```
  최상위 배열 길이 = 1          ← '덩어리' 개수
  [덩어리 1]
    그룹키: {alertname: "LabAlwaysFiring"}
    └─ 안에 든 알림: 2건        ← 화면에 '줄'로 보이는 것
         · LabAlwaysFiring | node-exporter:9100
         · LabAlwaysFiring | localhost:9090
  ```

  메일은 **`[FIRING:2]` 한 통**. 재개 후 새 메일은 오지 않았습니다(`repeat_interval: 1h` — 마지막 발송 05:55, 다음은 06:55경).

- **해석**: **"보이는 개수"와 "덩어리 개수"가 다릅니다.** 구조가 2층이라 생긴 혼동입니다.

  ```
  ┌─ LabAlwaysFiring ─────────────────┐   ← 덩어리 1개 (상자)
  │  · node-exporter:9100             │   ← 알림 1  (줄)
  │  · localhost:9090                 │   ← 알림 2  (줄)
  └───────────────────────────────────┘
  ```

  상자를 세면 1, 줄을 세면 2입니다. `instance` 가 다른 것은 맞지만 **묶는 기준이 `instance` 가 아닙니다.**

  ```yaml
  group_by: ["alertname"]      # alertname 만 보고 묶음
  ```

  둘 다 `alertname=LabAlwaysFiring` 이라 같은 상자입니다. `group_by: ["alertname", "instance"]` 였다면 2덩어리가 됐습니다.

  **⭐ 상자 개수 = 메일 통수입니다. 줄 개수가 아닙니다.** 제목의 `[FIRING:2]` 에서 `2` 가 묶인 알림 개수이고, 사람은 한 통을 받고 "이 알림이 2군데서 났구나"를 압니다.

  §3 에서 답한 **"50개가 모두 동일 이슈면 더더욱 문제"** 가 여기 그대로 적용됩니다.

  | `group_by` | 상자 | 사람이 받는 메일 |
  |---|---|---|
  | `[alertname]` | 1개 | **1통** `[FIRING:50]` |
  | `[alertname, instance]` | 50개 | **50통** |

  같은 50건인데 1통이냐 50통이냐로 갈립니다. **`group_by` 를 무엇으로 두느냐가 알림 피로를 만들기도 하고 막기도 합니다.** 실무에서는 "무엇을 한 사건으로 볼 것인가"를 정해 적습니다 — 클러스터 전체 장애면 `[alertname]`, 서버별로 따로 대응해야 하면 `[alertname, instance]`.

  판단 기준과 타이머 3종 상세는 **05-01 이 SSOT** 입니다.

### B-3 — 억제(inhibition)

**⏭️ 이번 세션 미실시 — 05-01 로 넘깁니다.**

critical 을 띄우려면 앱을 내려야 하는데, 그러면 D-2 에서 열어 둔 상태가 흐트러집니다. 규칙 자체는 `infra/alertmanager/alertmanager.yml` 에 넣어 두었습니다.

```yaml
inhibit_rules:
  - source_matchers: [severity = "critical"]
    target_matchers: [severity = "warning"]
    equal: ["instance"]
```

`equal: ["instance"]` 는 "같은 instance 일 때만 누른다"는 뜻입니다 — 없으면 A 서버의 critical 이 B 서버의 warning 까지 눌러 버립니다. 계획서도 B 를 **존재 증명까지**로 그어 두었고, 억제 동작 상세는 05-01 이 SSOT 입니다.

### B 묶음 정리

| # | 예측 | 결과 |
|---|---|---|
| B-1 | firing 유지 · 메일 안 옴 | ✅ 적중 (단 **방향은 push**, 예측의 "긁어간다"는 반대) |
| B-2 | 2덩어리 · 2통 | ❌ **1덩어리 · 1통** (`group_by` 가 상자를 정함) |
| B-3 | — | ⏭️ 미실시 (05-01) |
| §3 병목 | **사람** | ✅ **오답 교정 완료** (인출 때는 "기계") |

---

## 정리

```bash
rm -f infra/node-exporter/textfile/*.prom
docker compose -f infra/compose.yaml --profile prometheus down
docker compose -f infra/compose.yaml up -d      # ⚠️ 아래 함정 때문에 필요
```

> ⚠️ **`down` 은 profile 을 필터로 쓰지 않습니다.** `up` 과 달리 **프로젝트 전체**를 대상으로 하므로, `--profile prometheus down` 을 실행하면 Prometheus 스택뿐 아니라 **kafka·postgres·redis·opensearch 같은 앱 인프라까지 내려갑니다.** 이 세션에서 실제로 그렇게 됐고 `up -d` 로 복구했습니다.
>
> Prometheus 스택만 정확히 내리려면:
> ```bash
> docker compose -f infra/compose.yaml --profile prometheus stop \
>   prometheus node-exporter alertmanager pushgateway
> docker compose -f infra/compose.yaml --profile prometheus rm -f \
>   prometheus node-exporter alertmanager pushgateway
> ```

**최종 상태 확인 결과**

| 구분 | 상태 |
|---|---|
| Prometheus 스택 4개 | ✅ 전부 내려감 |
| 앱 인프라 7개 | ✅ 복구됨 |
| LGTM 설정 파일 | ✅ 무변경 (git 원본 그대로) |
| textfile 디렉토리 | ✅ 비움 |

## 이 실습을 위해 만든 설정 — 무엇을 왜 그렇게 두었나

파일은 실습 진행자가 미리 만들었습니다. 직접 작성하지 않았으므로 **어떤 판단이 들어갔는지**, 특히 **실습용이라 운영과 다르게 둔 곳**을 남깁니다.

### `infra/compose/prometheus.yaml`

| 설정 | 왜 |
|---|---|
| `profiles: ["prometheus"]` (전 서비스) | 이게 없으면 `docker compose up` 만 해도 딸려 올라옵니다. LGTM 과 분리해 켜고 끄려면 필수 |
| `extra_hosts: host.docker.internal:host-gateway` | 컨테이너에서 **호스트의 8092**(bootRun 앱)를 부르려고. Mac Docker Desktop 은 기본 제공하지만 Linux 는 없어 명시해야 이식됨 |
| `--web.enable-admin-api` · `--web.enable-lifecycle` | A-2 의 `/tsdb-status` 를 보려고 켰습니다. ⚠️ **운영에서는 꺼야 합니다** — 데이터 삭제 API 가 열립니다 |
| textfile 볼륨 `:ro` + `--collector.textfile.directory` | **두 줄이 짝**입니다. 마운트만 하고 옵션을 안 주면 node-exporter 가 그 디렉토리를 **안 읽습니다**. C-2 의 "왜 파일을 읽나"의 답이 이 옵션. `:ro` 는 exporter 가 파일을 쓸 일이 없어서 |

### `infra/prometheus/prometheus.yml`

| 설정 | 왜 |
|---|---|
| `job_name: notification-service-wrong-path` | **D-3 전용으로 일부러 넣은 실패 job**. 정상 job 과 같은 주소를 가리키되 `metrics_path` 만 뺐습니다. ⚠️ 실무 설정이라면 있으면 안 되는 항목 |
| `honor_labels: true` (pushgateway) | **C-5 의 핵심**. 없으면 Prometheus 가 push 된 `job="history-archive"` 를 **자기 것(`job="pushgateway"`)으로 덮어씁니다.** `true` 여야 배치가 보낸 정체가 보존됩니다 — C-5 에서 라벨이 갈린 것이 이 한 줄 덕입니다 |
| `scrape_interval: 15s` | 기본값. C-1 의 "하루 5,760회" 계산이 여기서 나옵니다 |

### `infra/prometheus/rules/lab-alerts.yml`

| 설정 | 왜 |
|---|---|
| `expr: up{...} == 1` | **일부러 항상 참인 조건**. ⚠️ 실무 룰이라면 `== 0`(죽었을 때)이어야 합니다 |
| `for: 10s` | B 를 빨리 보려고 짧게. ⚠️ 운영에서 이렇게 짧으면 **순간 스파이크마다 알림이 터집니다** |
| 두 룰의 `alertname` 을 동일하게 | **B-2 의 전제**. 이름이 달랐으면 그룹핑이 안 일어났습니다 |

### `infra/alertmanager/alertmanager.yml`

| 설정 | 왜 |
|---|---|
| `group_by: ["alertname"]` | **B-2 예측이 빗나간 이유가 이 한 줄**입니다. `instance` 를 넣지 않아 2건이 1덩어리가 됐습니다 |
| `repeat_interval: 1h` | 재개 후 새 메일이 안 온 이유. 타이머 3종 의미는 **05-01 이 SSOT** 라 값만 짧게 잡았습니다 |
| `smtp_smarthost: host.docker.internal:1025` | **이미 떠 있는 MailHog 재사용** — 새 수신 서비스를 안 늘리려고. `smtp_require_tls: false` 는 MailHog 가 인증·TLS 를 안 받기 때문 |
| `inhibit_rules` | B-3 용으로 넣었으나 **이번에 검증 안 함** |

### 앱 쪽 수정 두 곳

`build.gradle` 과 `application.yml` 은 **주석 처리한 상태로** 제공했습니다. D-1 이 404 를 보려면 없는 상태여야 하고, D-2 에서 관문이 둘이라는 것을 보려면 하나씩 풀어야 했기 때문입니다. 현재는 둘 다 열려 있습니다.

## 다음 세션으로 넘기는 것 (이 세션에서는 기록만)

### 시각화 필요

| # | 대상 | 담을 축 | 왜 필요한가 |
|---|---|---|---|
| 1 | **textfile vs Pushgateway 흐름** | ① 배치가 값을 놓는 자리(디스크 ↔ 메모리) ② 재시작 시 남는 것/사라지는 것 ③ 라벨을 누가 붙이는가 | C-5 를 글로만 읽으면 두 경로가 "둘 다 push 같다"에서 안 갈립니다. 나란히 그려야 갈립니다 |
| 2 | **프로세스 층 ↔ 부품 층** | `docker compose ps` 넷 vs `:9090` 화면 넷, 그리고 둘이 겹치지 않음 | §1·§2 오답의 뿌리가 이 한 장이면 정리됩니다 |
| 3 | **404 세 번, 이유 세 가지** | D-1(빈 없음) → D-2 관문①(노출 안 됨) → D-3(경로 어긋남) | 같은 숫자가 다른 원인을 가리는 것을 순서로 보여야 합니다 |

배치 경로는 `_assets/` 아래, 스타일은 기존 학습 노트 SVG 관례(다크 배경 `#0d0d0d` + 골든앰버)를 따릅니다.

### 노트 보강 대상 (`_todo-02-01-실습보강.md` 로 별도 작성 예정)

02-01 문서에 **없는** 사실만 추립니다. 노트 파일(`_mistakes.md` 등)은 이 세션에서 건드리지 않았습니다.

| # | 보강할 내용 | 근거 |
|---|---|---|
| 1 | `/actuator/prometheus` 404 의 **이유가 둘**(빈 없음 / 노출 안 됨). 문서는 "명시적으로 노출해야 한다"까지만 말하고 두 층을 안 가름 | D-1·D-2 |
| 2 | `/actuator/metrics` 가 **대조군** — 빈은 있는데 `include` 에 없어 404. `prometheus` 와 나란히 놓으면 두 층이 갈림 | D-2 |
| 3 | `metrics_path` 누락은 에러가 아니라 **기본값 `/metrics` 사용**. 조용히 엉뚱한 곳을 긁다 404 로 드러남 | D-3 `lastError` |
| 4 | Content-Type `text/plain;version=0.0.4` — 노출 형식에 버전이 박힘 | D-2 |
| 5 | `include` 는 화이트리스트. `"*"` 는 `env`(비밀번호)·`heapdump`(메모리 내용) 노출 위험 | 곁가지 |
| 6 | textfile 값에 **`job` 라벨을 Prometheus 가 붙임** — 배치가 자기 이름을 잃음. push 는 URL 에 박아 보존 | C-2·C-5 |
| 7 | Pushgateway 재시작 시 **과거 시계열은 TSDB 에 남고 미래만 끊김** — "다 사라진다"가 아님 | C-5 시계열 |
| 8 | 컨테이너 재시작 실험의 **측정 함정** — 기동 완료 전 조회를 "값 사라짐"으로 오판 가능. `StartedAt`·`RestartCount` 대조 필요 | C-5 진행 중 실제 오판 |
| 9 | Prometheus → Alertmanager 는 **push**. 생태계에서 거의 유일한 push 경로이고 설정도 `scrape_configs` 가 아니라 `alerting` 블록 | B-1 예측 방향 오류 |
| 10 | 그룹핑은 **상자와 줄이 2층** — 화면의 줄 수가 아니라 **상자 수가 메일 통수**. `group_by` 가 상자를 정함 | B-2 예측 오류 |

### 이 프로젝트에 남기는 사실 (실습과 별개)

| # | 발견 | 조치 |
|---|---|---|
| 1 | **Loki·Mimir·Tempo 설정도 빈 스텁** — `config.alloy` 뿐 아니라 셋 다 주석만 있어 기동 즉시 죽습니다. `--profile observability up` 은 현재 **Grafana·Alloy 만** 살아남습니다 | 3-2 주차에 넷을 함께 채워야 함. 계획 수립 시 `config.alloy` 만 확인하고 나머지 셋을 안 열어 본 것이 이 세션의 조사 누락 |
| 2 | **`docker compose --profile X down` 이 프로젝트 전체를 내림** | 위 「정리」의 `stop` + `rm` 형태 사용 |

## 재방문 트리거 대조 (`_mistakes.md` 41행의 6개)

이 세션에서 **노트 파일은 건드리지 않았습니다.** 대조 결과만 남깁니다.

| # | 트리거 | 이 실습에서 |
|---|---|---|
| 1 | Prometheus 스택 넷 vs LGTM 넷, 축이 어떻게 다른가 · 겹치는 하나 | ✅ **A-4 로 해소** — 축(메트릭 역할 분담 ↔ 신호 종류)과 겹치는 하나(메트릭)를 자력으로 답함 |
| 2 | 내부 4부품 · 무엇이 중심이고 나머지 셋과의 관계 | ✅ **A-2 로 해소** — Alertmanager 를 답에 넣는 층 혼동이 `:9090/alertmanager` 404 로 교정됨. TSDB 중심 구조도 정리 |
| 3 | 스위치 1대로 서버 50대 알림 시 **병목이 무엇인가** | ✅ **B-1 문답에서 자력 교정** — "사람"으로 답함. 단 Alertmanager **도구 넷 중 실무 기본**은 아직 안 물음 |
| 4 | ⭐ **파일 쓰기 vs Pushgateway** (최우선) | ✅ **C-5 로 해소** — 축 셋(값의 수명·정체·노드 결속)으로 갈림. **미해결 물음 종결** |
| 5 | `up` 이 무엇을 기준으로 1/0 · 파일이 깨지면 노드 메트릭은 | ✅ **C-3·C-4 로 해소** — `up=1` 과 `scrape_error=1` 이 동시에 참인 것을 실측 |
| 6 | 왜 하필 **Node Exporter** 가 textfile collector 를 갖는가 | ⏳ **미해소** — C-2 에서 "리눅스가 `/proc` 파일로 상태를 노출하고 node-exporter 가 그것을 읽는 프로그램"까지는 나왔으나, *왜 하필 그 exporter 인가*는 정면으로 안 물음. **08-01(Node Exporter 해부)** 에서 다룰 자리 |

## 마친 뒤 할 일

- [ ] 오답 노트 `_mistakes.md` 2026-07-28 02-01 항목에 **4번 해소** 실측 근거를 덧붙입니다(별도 세션).
- [ ] `_todo-02-01-실습보강.md` 를 새로 만들어 위 「노트 보강 대상」 8항목을 옮깁니다(별도 세션). ⚠️ 같은 이름의 1차 보강 TODO 는 오늘 이미 반영·삭제됐으므로(커밋 `e031b61c`) 파일명을 구분합니다.
- [ ] 시각화 3건(위 표) 작성.
