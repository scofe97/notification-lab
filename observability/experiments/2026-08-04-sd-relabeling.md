# 서비스 디스커버리와 relabeling 실습 — 04-01 문서 블록 대조

2026-08-04 · 《Mastering Prometheus》 4장(Using Service Discovery) Phase 3 실습 기록입니다.

정본 문서: `~/runners-high/write/06_observability/book/mastering_prometheus/04-01.서비스 디스커버리와 relabeling.md` (579줄)

## 이 기록의 규약 위반을 먼저 적습니다

07-28 기록은 **예측 → 실제 → 해석** 셋을 규약으로 두고 "예측 칸을 비운 채 실행하지 않는다"고 못 박았습니다. **이번 세션은 그 규약을 지키지 못했습니다.** 예측을 먼저 적지 않고 실행했으므로, 아래 항목은 대부분 예측 칸이 비어 있습니다.

이유를 감추지 않고 적습니다. 세션 중반까지 실습이 **문서와 무관한 방향으로 샜고**(kiada → coredns → helm 차트 내부), 그것을 되돌려 문서 블록 기준으로 재설계하는 데 시간을 썼습니다. 재설계 후에는 예측을 적을 여력이 남지 않았습니다.

그래서 이 기록의 값어치는 "예측 적중률"이 아니라 **문서 블록과 실물의 대조**에 있습니다.

> **덧붙임 (같은 날 16:00~16:37)** — 이어서 진행한 §S6·S7 은 **예측을 먼저 적고 실행했습니다.** 규약을 그 자리에서 복원했고, 그 덕에 P3 반증과 P5 의 조건부 성립을 잡았습니다. 예측을 안 적었으면 "문서가 틀렸다"로 끝났을 자리입니다.

### 결과 요약

문서 코드블록 **여섯 개 중 다섯 개**를 실물로 확인했습니다.

| 문서 행 | 블록 | 단계 | 결과 |
|---|---|---|---|
| 83 §2 | ServiceMonitor (`port`·`jobLabel`·`attachMetadata`) | S1 | ✅ 클러스터에 **이미 존재**. 6필드 중 5개 문서와 동일 |
| 162 §3 | `relabelings`/`metricRelabelings` (camelCase) | S2 | ✅ 번역 확인 + 시계열 감소 355개 |
| 136 §3 | `k8s-pods` — 두 시점 한 job | S3 | ✅ keep 0 → 3 |
| 191·203 §4 | `job` 덮어쓰기 | S4 | ✅ 빈 결과 vs 4,966 |
| 306·337 §8 | `http_sd` YAML↔JSON | S5 | ✅ 타깃 3개 UP |
| §6 계약 | 빈 배열 vs 오류 | S6·S7 | ✅ 이어서 완료 — 예측 7개 중 5 적중·1 반증·1 조건부 |
| 399 Spring | `eureka_sd_configs` | — | ⏭️ Eureka 서버 없음 |

**오진 4회** — 전부 "간접 지표로 단정"이었습니다. §오진 기록에 따로 적습니다.

---

## 사전 상태 (실측)

| 항목 | 값 |
|---|---|
| kind 클러스터 | `k8s-lab` 3노드 v1.35.0, context `kind-k8s-lab`, 28일 경과 |
| helm | v4.1.4, `prometheus-community` repo 등록 |
| 설치 | kube-prometheus-stack **87.15.1** (appVersion v0.92.1), release `kube-prom`, ns `mon-lab` |
| 설치 옵션 | `grafana.enabled=false`, `scrapeInterval=15s`, `serviceMonitorSelectorNilUsesHelmValues=false`, `podMonitorSelectorNilUsesHelmValues=false` |
| CRD | 설치 전 0개 → 후 **10개** |
| 파드 | 7개 (operator·prometheus·alertmanager·kube-state-metrics·node-exporter ×3) |
| 접속 | `localhost:19090` (port-forward). 9090 은 무관한 `prom` 컨테이너가 점유 — 미접촉 |
| cadvisor 원시 시계열 | control-plane 2,578 · worker 2,758 · worker2 2,561 |
| 전체 파드 | 36개 (default 15). `prometheus.io/*` 어노테이션 보유 **0개** |

---

## S1 — 문서 83행 블록이 클러스터에 이미 있었다

**실제**: 문서 예제와 살아 있는 오브젝트가 필드별로 일치합니다.

| 문서 83행 | `kube-prom-prometheus-node-exporter` | |
|---|---|---|
| `attachMetadata: {node: false}` | `{'node': False}` | ✅ |
| `port: http-metrics` | `http-metrics` | ✅ |
| `scheme: http` | `http` | ✅ |
| `jobLabel: jobLabel` | `jobLabel` | ✅ |
| `app.kubernetes.io/name: prometheus-node-exporter` | 동일 | ✅ |
| `app.kubernetes.io/instance: mastering-prometheus` | `kube-prom` | 릴리스명만 다름 |
| — | `relabelings: 0`, `metricRelabelings: 0` | **비어 있음** |

**해석**: 문서 저자가 `mastering-prometheus` 로, 우리가 `kube-prom` 으로 helm install 한 차이뿐입니다. 새로 쓸 YAML 이 0줄이고, `relabelings` 가 비어 있다는 것이 S2 의 출발점이 됩니다.

⭐ **이 단계가 세션 초반 실패의 교정 지점입니다.** 문서 예제가 클러스터에 이미 있는데 kiada·coredns 를 찾아다녔습니다.

---

## S4 — 문서 191행 `job` 덮어쓰기는 2단이었다

**실제**: 문서가 `...` 로 줄인 부분이 실물에서 펼쳐졌습니다.

```
문서:  source_labels: [__meta_kubernetes_service_label_app_kubernetes_io_instance, ...]
       regex: (mastering-prometheus);true

실물:  source_labels: [__meta_kubernetes_service_label_app_kubernetes_io_instance,
                       __meta_kubernetes_service_labelpresent_app_kubernetes_io_instance]
       regex: (kube-prom);true
```

`;true` 의 정체는 **`labelpresent`** 입니다. 두 소스가 `;` 로 이어져 `kube-prom;true` 가 되고, "값이 kube-prom 이며 그 라벨이 존재한다"를 한 정규식으로 검사합니다. `selector.matchLabels` 가 둘이니 `keep` 도 둘로 번역됐습니다.

그리고 `job` 을 덮어쓰는 규칙이 **둘**입니다.

```
12. __meta_kubernetes_service_name            → job   (regex 없음 = 무조건)
14. __meta_kubernetes_service_label_jobLabel  → job   (regex: (.+) = 값 있을 때만)
```

입력부터 최종값까지 실측으로 이어집니다.

```
설정의 job_name   serviceMonitor/mon-lab/kube-prom-prometheus-node-exporter/0
      ↓ 1차 (무조건)
Service 이름       kube-prom-prometheus-node-exporter
      ↓ 2차 (jobLabel 값 있음)
Service jobLabel   node-exporter
      ↓
최종 job 라벨       node-exporter
```

**증거**:

```promql
count({job="serviceMonitor/mon-lab/kube-prom-prometheus-node-exporter/0"})  → 빈 결과
count({job="node-exporter"})                                               → 4,966
```

`job` 값 목록에 `kube-prom-prometheus-node-exporter` 가 **없습니다** — 1차 결과가 2차에 완전히 덮여 중간값은 어디에도 남지 않습니다.

**해석**: 문서 §4 의 "그 이름으로 시계열을 찾으면 아무것도 나오지 않는다"가 3단으로 설명됩니다. 문서는 2차 규칙만 보여 주므로, `jobLabel` 을 지정하지 않았다면 1차 결과(Service 이름)가 최종값이 된다는 것은 실물에서만 보입니다. 문서 숫자 2,234 와 우리 4,966 의 차이는 node-exporter 버전에 따른 collector 수 차이로 봅니다.

---

## S2 — 문서 162행: camelCase 가 번역되고, 내 규칙은 뒤에 붙는다

**파일**: `infra/k8s/servicemonitor-node-exporter-relabelings.yaml`

Operator 가 만든 SM 을 고치지 않고 **별도 SM** 으로 같은 Service 를 대상 삼았습니다. helm 관리 오브젝트를 손으로 바꾸면 다음 upgrade 에 덮어써져 원인을 못 찾기 때문입니다.

**실제 1 — 번역**: 문서 155~157행 대응표대로 됐습니다.

```
sourceLabels       → source_labels
targetLabel        → target_label
relabelings        → relabel_configs
metricRelabelings  → metric_relabel_configs
```

**실제 2 — 순서**: 문서 173행("사용자 규칙을 자기 규칙 뒤에 이어붙인다")이 맞았습니다. 다만 **맨 뒤는 아닙니다.**

```
 1~13.  Operator 생성 (keep 3 · 라벨 승격 7 · drop 1 · job 덮어쓰기 1 · endpoint 1)
14~15.  ★ 내 규칙 (jobLabel→job, lab_source 표지)
16~20.  샤딩 규칙 5개  ← 내 규칙 뒤에 또 붙음
```

문서는 샤딩까지 언급하지 않습니다. 동작에 영향은 없으나(샤딩은 타깃 분배용) 기록해 둘 차이입니다.

**실제 3 — `metricRelabelings` 효과**: 같은 Service, 같은 3개 타깃, 규칙만 다릅니다.

| | 내 job | Operator job |
|---|---|---|
| 타깃 | 3 | 3 |
| `node_scrape_collector_*` | **0** | 294 |
| 전체 시계열 | 4,627 | 4,966 |

**해석**: 같은 대상을 긁는데 저장된 시계열이 339개 다릅니다. `metric_relabel_configs` 의 정체가 이것입니다 — 스크레이프는 똑같이 하고 저장 직전에 버립니다. **버려도 스크레이프 비용은 이미 냈다**는 문서의 지적이 여기서 확인됩니다.

⭐ **`lab_source` 표지 한 줄이 결정적이었습니다.** 두 job 의 `job` 라벨이 모두 `node-exporter` 로 덮여 구별이 불가능했고, `count(up{job="node-exporter"})` 이 6(=3+3)으로 나와 제가 한 번 오진했습니다. `sourceLabels` 없이 `targetLabel`+`replacement` 만 쓰면 상수 라벨이 붙는다는 것도 이 줄에서 확인됩니다.

---

## S3 — 문서 136행: 두 시점을 한 job 에서

**파일**: `infra/k8s/additional-scrape-configs.yaml` (문서 블록 그대로)

`additionalScrapeConfigs` 통로를 쓴 이유는 ServiceMonitor 를 쓰면 Operator 가 규칙을 대신 써(S2 에서 20개 확인) 배울 대상이 사라지기 때문입니다.

**실제**:

| | active | dropped |
|---|---|---|
| 어노테이션 부여 전 | **0** | 53 |
| 부여 후 (`kubectl annotate pod -l app=kiada prometheus.io/scrape=true`) | **3** | 50 |

`count(up{job="k8s-pods"})` 0 → **3**.

타깃 주소가 `10.244.1.5` 로 **포트가 없습니다.** kiada 파드에 `ports:` 선언이 없어 `__address__` 에 포트가 붙지 않았고, 기본 80 을 시도해 `dial tcp` 거부로 `up=0` 이 됐습니다.

**해석**: `keep` 의 목적은 "어노테이션 붙은 파드만 고르기"이고 그건 달성됐습니다. 스크레이프 성공은 별개 층입니다. 그리고 `up=0` 인데 **시계열은 3개** — `up` 은 Prometheus 가 스스로 만들므로 실패도 "0"이라는 샘플로 남습니다(03-04 §5 와 동일 구조).

`metric_relabel_configs` 의 `go_gc_duration_seconds.*` drop 은 **이 job 에서 걸리지 않았습니다.** kiada 가 메트릭을 내지 않아 버릴 것이 없습니다.

```
count({job="k8s-pods"})  =  18     ← up 3개 + Prometheus 가 붙이는 메타 15개
그중 go_gc_duration_seconds*  =  0   ← 앱 메트릭이 0개니 당연
```

클러스터 전체에는 `go_gc_duration_seconds` 가 140개 있지만 **전부 다른 job 소속**이라 이 규칙과 무관합니다. `metric_relabel_configs` 는 그 job 이 긁어온 메트릭에만 적용됩니다.

### ⭐ 효과가 보이는 대상으로 옮겨 재측정

규칙만 있고 효과가 0 인 상태로 두면 "두 시점이 다르다"를 숫자로 확인할 수 없습니다. 그래서 같은 규칙을 `http_sd_test` job 에 걸었습니다 — 그 job 은 node-exporter 파드 3개를 긁으므로 실제로 버릴 것이 있습니다.

| | `http_sd_test` (규칙 2개) | `node-exporter` (대조군, 규칙 없음) |
|---|---|---|
| 타깃 | 3 (같은 파드) | 3 (같은 파드) |
| 전체 시계열 | **4,634** | 5,006 |
| `go_gc_duration_seconds*` | **0** | 21 |
| `node_scrape_collector_*` | **0** | 294 |

**같은 파드를 긁는데 저장된 시계열이 372개 다릅니다.** drop 두 규칙이 315개를 걷어냈고 나머지는 스크레이프 시점 차이입니다.

이 대조가 §3 의 명제를 숫자로 만듭니다 — **스크레이프는 똑같이 하고 저장 직전에 버립니다.** 네트워크 비용은 이미 냈고 디스크만 아낍니다. 실제 카디널리티 튜닝은 문서가 7장으로 미루지만, 두 시점이 다르다는 사실 자체는 여기서 확인됩니다.

⭐ **반영 지연에 또 걸렸습니다.** Secret 을 고쳤는데 조회가 옛 값을 보여 "미반영"으로 세 번 판단했습니다. 실제로는 Operator 가 설정을 다시 만들고 → Secret 이 파드 볼륨에 갱신되고 → config-reloader 가 감지해 리로드하기까지 **1분 남짓** 걸립니다. 리로드 로그의 타임스탬프(`06:16:48`)를 보고서야 확인됐습니다. 오진 목록의 1·4번과 같은 원인입니다.

---

## S5 — 문서 306·337행: http_sd

**파일**: `infra/k8s/http-sd-server.yaml` (nginx + ConfigMap), `additional-scrape-configs.yaml` 의 `http_sd_test` job

문서 §8 요구사항 셋을 검증했습니다.

```
HTTP/1.1 200 OK                  ← 요구사항 1
Content-Type: application/json   ← 요구사항 2
charset utf-8                    ← 요구사항 3
```

응답은 문서 317행 블록과 같은 모양입니다. 다만 문서의 `web1.example.com:443` 은 실재하지 않아, 이 클러스터의 node-exporter 파드 IP 로 바꿨습니다.

**실제**: 타깃 3개 전부 `health=up`, `labels` 가 그대로 붙었습니다.

```
172.21.0.2:9100  up  component=node-exporter  sd_source=http_sd
172.21.0.3:9100  up  component=node-exporter  sd_source=http_sd
172.21.0.4:9100  up  component=node-exporter  sd_source=http_sd
```

**해석**: `http_sd` 는 **주소 목록만** 줬는데 메트릭이 들어옵니다. 목록은 SD 가, 값은 각 node-exporter 가 냈습니다 — 문서 §1 의 "SD 는 어디로 갈지만 담당한다"가 UP 으로 확인된 자리입니다.

`/targets-empty` 경로를 함께 만들어 두었습니다. 문서 §8 의 "타깃이 없을 때도 200 과 빈 배열" 을 확인하려면 job 의 `url` 을 그 경로로 바꿔 붙입니다. 이번 회차에서는 실행하지 않았습니다.

---

## S6·S7 — "없다" 와 "모른다" 의 구별 (이어서 진행, 16:00~16:37)

앞 회차가 남긴 `/targets-empty` 를 실행하고, 오류 응답 쪽까지 마저 봤습니다. **이번에는 예측을 먼저 적고 실행했습니다** — 07-28 규약 복원입니다.

문서 §6 의 계약은 이렇습니다. 빈 배열은 "조회했고 0대"이고 오류는 "조회에 실패해 모른다"이며, Prometheus 는 후자일 때 직전 목록을 계속 씁니다.

### 예측과 결과

| # | 예측 | 결과 | 실제 |
|---|------|------|------|
| P1 | 빈 배열 → 타깃 3개가 전부 사라진다 | ✅ | 3 → 0 |
| P2 | 반영까지 1분 내외 | ✅ | Secret 적용 15:59:05 → 리로드 15:59:49 → 타깃 0 이 16:00:00 |
| P3 | `up` 시계열은 5분 lookback 동안 남는다 | ❌ | **즉시 0개.** staleness marker 가 lookback 을 무시하고 시계열을 끊음 |
| P4 | 빈 배열은 실패 카운터를 안 올린다 | ✅ | `prometheus_sd_http_failures_total` 그대로 0 |
| P5 | 오류 → 타깃이 그대로 남는다 | ⚠️ | **조건부.** 아래 §갈린 자리 참조 |
| P6 | 오류 → 실패 카운터가 오른다 | ✅ | 0 → 6 |
| P7 | `/targets` 화면에 SD 실패가 안 드러난다 | ✅ | 2분 넘게 실패 중인데 3개 다 `up=1`, 스크레이프도 계속됨 |

### P3 이 틀린 이유 — staleness marker

빈 배열로 타깃이 사라진 직후 `up{job="http_sd_test"}` 를 물으면 **0개**입니다. "5분 lookback 동안은 마지막 값이 조회된다"고 예측했는데 아니었습니다.

타깃이 사라질 때 Prometheus 는 그 시계열에 **staleness marker** 를 씁니다. 이 표식은 lookback 을 덮어써 순간 질의에서 시계열을 즉시 끊습니다. 다만 과거가 지워지는 것은 아니어서 range 질의로는 그대로 보입니다.

```
up{job="http_sd_test"}      → 0개          (순간 질의)
up{job="http_sd_test"}[6m]  → 3개, 각 22샘플, 15:54:21~15:59:47
```

lookback 은 *수집이 늦어질 때* 마지막 값을 보여 주는 장치이지, *대상이 사라졌을 때* 유지하는 장치가 아닙니다. 둘을 같은 것으로 본 게 예측 오류의 원인입니다.

### ⭐ P5 가 갈린 자리 — 오류가 "언제" 났는가

처음엔 오류를 넣었더니 타깃이 **0으로 떨어졌습니다.** 문서와 반대라 재현 조건을 갈랐더니 갈림길이 나왔습니다.

| 오류가 난 시점 | 직전 목록 | 실측 |
|---|---|---|
| 설정 리로드 직후 **첫 조회** | 유지할 목록이 **없음** → 타깃 0 | 16:14 회차, 실패 1~3 인데 타깃 0 |
| 이미 목록이 있는 상태의 **주기 갱신** | **유지됨** | 16:35 회차, 실패 4→6 인데 타깃 3 유지 |

Prometheus 로그가 두 경로를 다른 줄 번호로 찍어 알아챘습니다.

```
refresh.go:72  ← 최초 조회 (Run 진입 시 1회)
refresh.go:91  ← 주기 갱신 (ticker 루프)
```

v3.13.1 소스가 그대로 설명합니다. `Run()` 은 진입하자마자 한 번 조회하고, 실패하면 로그만 남기고 채널로 아무것도 보내지 않습니다. 이후 ticker 루프에서 실패하면 `continue` 로 **채널 송신을 건너뜁니다** — 그래서 앞서 보낸 목록이 소비 측에 그대로 남습니다.

즉 **"직전 목록 유지"는 직전 목록이 존재할 때의 이야기**입니다. url 을 바꾸면 설정 리로드가 SD 를 새로 띄우므로 그 목록이 리셋되고, 첫 조회가 실패하면 남길 것이 없습니다. 처음 실험이 이 경우였습니다.

문서가 틀린 것이 아니라 **실험 설계가 문서의 전제를 깬 것**입니다. 이걸 확인하려고 두 번째는 Prometheus 설정을 건드리지 않고 **nginx 쪽만** 500 으로 바꿨습니다. 그때 비로소 문서대로 유지됐습니다.

### P7 — 증상이 없다는 것이 증상입니다

두 번째 회차에서 SD 는 2분 넘게 실패 중이었는데 화면은 이렇습니다.

```
172.21.0.2:9100  up  lastScrape=07:36:51
172.21.0.3:9100  up  lastScrape=07:36:55
172.21.0.4:9100  up  lastScrape=07:37:02
up{job="http_sd_test"} → 1, 1, 1
```

`/targets` 만 보면 정상입니다. 목록이 얼어붙었을 뿐 얼어붙은 목록을 계속 잘 긁고 있기 때문입니다. 문서 §6 이 "스스로 포기하지 않으니 증상도 없다"고 적고 알림을 걸라고 한 이유가 이 화면입니다.

```promql
rate(prometheus_sd_http_failures_total[5m]) > 0
```

### ⭐ 알림 메트릭 — 처음 적은 결론을 뒤집습니다

문서 본문은 `prometheus_sd_refresh_failures_total` 을 알림 근거로 제시합니다. 실습 직후 저는 "이 클러스터에서 오른 것은 `prometheus_sd_http_failures_total` 뿐이고 전자는 0개 시계열"이라고 적었는데, **틀렸습니다.** 순간 질의만 보고 단정한 것이라 지난 회차 오진 2 와 같은 실수입니다.

range 질의로 다시 보면 전자도 **존재했습니다.**

```promql
prometheus_sd_refresh_failures_total{config="http_sd_test"}[3h]
  → labels: mechanism="http", config="http_sd_test"
    14:00 값=0  →  15:59 값=0   (샘플 417개)

prometheus_sd_http_failures_total[3h]
  → labels 없음
    13:43 값=0  →  16:42 값=6   (샘플 545개)
```

갈린 이유가 둘입니다.

**첫째, 자료형이 다릅니다.** `refresh_failures` 는 `mechanism`·`config` 라벨을 가진 **CounterVec** 이고, `http_failures` 는 라벨 없는 **plain Counter** 입니다(v3.13.1 `discovery/metrics_refresh.go` 38행, `discovery/http/metrics.go` 37행).

**둘째, 설정 리로드가 CounterVec 을 리셋합니다.** `refresh_failures` 의 샘플이 **15:59 에서 끊깁니다** — 빈 배열 실험으로 url 을 바꿔 설정을 리로드한 그 시각입니다. SD 인스턴스가 새로 뜨면서 그 라벨 조합의 시계열이 사라졌고, 이후 오류(16:14~16:37)는 새 시계열에 쌓였습니다. 반면 라벨 없는 `http_failures` 는 리로드를 건너 살아남아 0→6 이 됐습니다.

소스상 **두 카운터는 같은 오류에서 함께 오릅니다.** `refresh.go:116` 의 `d.metrics.Failures.Inc()` 와 `http.go:173` 의 `d.metrics.failuresCount.Inc()` 가 같은 실패 경로에 있습니다. 어느 하나만 오르는 구조가 아닙니다.

실무 함의는 이렇습니다. **`config` 라벨이 붙은 카운터로 알림을 걸면 설정 리로드 때 시계열이 갈아엎히므로 `rate()` 가 끊깁니다.** `increase()`·`rate()` 는 카운터 리셋을 보정하지만, 리셋이 아니라 *시계열 자체가 교체*되는 경우는 다릅니다. 둘 다 걸어 두는 편이 안전합니다.

```promql
rate(prometheus_sd_http_failures_total[5m]) > 0
rate(prometheus_sd_refresh_failures_total{mechanism="http"}[5m]) > 0
```

### 남은 과제 — Content-Type 위반은 미검증

문서 §6 의 요구사항 셋 중 **`Content-Type: application/json`** 위반만 실측하지 못했습니다. `/targets-badtype` 경로(유효한 JSON 을 `text/plain` 으로 반환)를 `infra/k8s/http-sd-server.yaml` 에 만들어 뒀으나 실행 전 중단했습니다.

소스에는 전용 실패 분기가 있습니다 — `discovery/http/http.go` 가 응답의 Content-Type 을 `matchContentType` 으로 검사하고 어긋나면 `failuresCount` 를 올린 뒤 오류를 반환합니다. 200 이고 JSON 이 유효해도 거부될 것으로 보이지만, **실측하지 않았으므로 단정하지 않습니다.** 다음 회차 과제로 남깁니다.

### 환경 복원

`/targets` 정상 응답으로 되돌렸고 타깃 3개 전부 `up` 을 확인했습니다. `infra/k8s/` 파일도 원래 상태입니다.

---

## ⭐ 오진 기록 — 4회, 원인은 하나

전부 **간접 지표로 단정**한 것입니다. 되풀이하지 않게 적어 둡니다.

| # | 내 판단 | 실제 | 무엇을 봐야 했나 |
|---|---|---|---|
| 1 | "리로드가 안 됐다" | 됐음. 리로드 직전 스냅샷을 봄 | 리로드 로그의 타임스탬프 |
| 2 | "타임스탬프가 현재 시각이니 수집 중" | `count()` 는 **평가 시각**을 찍음 | 원본 샘플을 `[range]` 로 조회 |
| 3 | "6분 지났는데 안 사라진다" | 1분밖에 안 지났음 | 루프 경과를 곱셈으로 추정한 것이 오류 |
| 4 | "`additionalScrapeConfigs` 반영 안 됨" | 됐음. 또 직전 스냅샷 | Secret 을 직접 확인 |

1·4 는 같은 실수입니다. 폴링 루프가 조건 불만족으로 빠르게 소진되고, 그 결과를 "반영 안 됨"으로 읽었습니다. **루프에 실제 대기가 없었던 것**이 근본 원인입니다.

2 는 PromQL 이해의 결손입니다. 집계 함수의 결과 타임스탬프는 원본 샘플의 수집 시각이 아니라 **질의 평가 시각**입니다. stale 판별에 쓸 수 없습니다.

이 넷 다 04-01 §5 가 가르친 것과 같은 종류입니다 — **무엇을 보고 판단하는가.** `/targets` 만 보면 없는 것을 못 보고, 간접 지표만 보면 원인을 못 찾습니다.

---

## 부수 발견

**`__tmp_prometheus_job_name` 은 규칙 간 전달용이 아닙니다.** 생성 설정에서 `target_label` 로 **17회** 쓰이는데 `source_labels` 로는 **0회**입니다. 아무 규칙도 읽지 않습니다. Prometheus 내부가 "이 타깃이 어느 job 설정에서 왔는가"를 표시할 때 쓰는 값으로 보입니다. 문서 §심화가 말한 "다음 단계의 입력으로만 잠깐 보관"과는 다른 용례입니다.

반면 `__tmp_hash`·`__tmp_current_shard`·`__tmp_disable_sharding` 셋은 실제로 규칙 사이에서 참조됩니다(샤딩, 06-01 주제).

**같은 대상을 두 job 이 긁으면 시계열이 두 벌 됩니다.** 실습 중 coredns ServiceMonitor 를 우리 것과 Operator 것 둘로 만들어 `coredns_build_info` 가 파드 2개에 시계열 4개가 됐습니다. 오류가 없어 눈치채기 어렵고, 대시보드에서 합계가 두 배로 나옵니다.

**kiada 는 ServiceMonitor 대상이 될 수 없습니다.** 파드에 `ports:` 선언이 없어 Operator 가 `targetPort: 8080` 을 `keep(__meta_kubernetes_pod_container_port_number, regex: 8080)` 으로 번역하는데 그 메타 라벨이 아예 없어 발견된 16개가 전부 버려졌습니다. `/targets` 는 0개만 보여 주고 원인을 말하지 않았고, **dropped 목록에서 찾았습니다.**

---

## 남은 것

| 항목 | 다음 |
|---|---|
| `eureka_sd_configs` (399행) | Eureka 서버 필요. notification-lab 에 없음 — 세우면 배보다 배꼽 |
| `/targets-empty` 로 빈 배열 확인 | job `url` 만 바꿔 붙이면 됨 |
| cadvisor 카디널리티 감량 | 문서가 7장으로 미룸. 12,365 시계열이 대기 중 |
| 예측 규약 복원 | 다음 회차부터 예측 칸을 먼저 채운다 |

## 정리 명령

```bash
kubectl --context kind-k8s-lab -n default annotate pod -l app=kiada prometheus.io/scrape- --overwrite
kubectl --context kind-k8s-lab -n mon-lab delete servicemonitor node-exporter-relabelings-lab
kubectl --context kind-k8s-lab -n mon-lab delete secret additional-scrape-configs
kubectl --context kind-k8s-lab delete -f infra/k8s/http-sd-server.yaml
helm --kube-context kind-k8s-lab uninstall kube-prom -n mon-lab
kubectl --context kind-k8s-lab delete ns mon-lab
kubectl --context kind-k8s-lab get crd | grep monitoring.coreos   # helm uninstall 로 안 지워짐
```

`prom`:9090 은 처음부터 손대지 않았습니다.

## 실습 후 compose 정리 (같은 날)

이 실습으로 compose 쪽과 중복이 생겨 걷어냈습니다.

| 대상 | 처분 | 근거 |
|---|---|---|
| compose `prometheus`·`alertmanager` | 삭제 | helm 이 v3.13.1·v0.33.1 로 올림 — 진짜 중복 |
| compose `node-exporter` | **남김** | helm 쪽에 `--collector.textfile.directory` 인자 **없음**(실측). 02-01 C-2·C-3 textfile 실습이 이것에 달림 |
| `infra/prometheus/`·`infra/alertmanager/` | 삭제 | Operator 가 설정을 생성하므로 사람이 쓰는 설정 파일이 불필요 |
| Loki·Mimir·Tempo | 삭제 | 설정이 주석만 있는 실질 0줄 스텁. 기동 즉시 죽음(07-28 292행). ROADMAP 3-2 에 되살림 |
| `pushgateway`·`alloy`·`grafana` | 남김 | helm 에 없거나(pushgateway) 살아 있음 |
| `infra/compose/prometheus.yaml` | → `pushgateway.yaml` 로 개명 | 담긴 것이 둘로 줄어 이름이 실물과 어긋남 |

`--profile prometheus` 는 `--profile pushgateway` 가 됐습니다. 07-28 기록 머리에 대응표를 덧붙였습니다 — 본문은 그날의 사실이므로 고치지 않았습니다.

`infra/k8s/` 이름은 유지했습니다. 지금 담긴 셋이 helm 차트가 아니라 **생 k8s 매니페스트**(ServiceMonitor·Secret 원본·Deployment)이기 때문입니다. helm values 를 버전 관리할 때 `infra/helm/` 을 따로 만듭니다.

차트 구조를 다시 보려면:

```bash
helm pull prometheus-community/kube-prometheus-stack --version 87.15.1 --untar
```

`_chart/` 는 `.gitignore` 로 제외했습니다(외부 사본 301파일 7.1MB).
