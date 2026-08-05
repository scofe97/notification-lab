# Alertmanager 라우팅·억제 실습 — 05-01 문서 대조

2026-08-05 · 《Mastering Prometheus》 5장(Effective Alerting with Prometheus) Phase 3 실습 기록입니다.

정본 문서: `~/runners-high/write/06_observability/book/mastering_prometheus/05-01.Alertmanager — 라우팅·그룹핑·억제·HA.md`

## 규약 준수 여부

07-28 기록이 정한 **예측 → 실제 → 해석** 규약을 이번 세션은 전 항목에서 지켰습니다. 08-04 기록이 "예측 칸을 비운 채 실행했다"고 자기 비판을 남겼던 그 규약입니다.

예측을 먼저 적은 덕에 얻은 것이 하나 있습니다. `namespace=` 판정에서 결과가 예측과 **일치했는데도 근거로 쓸 수 없다**는 판단을 내릴 수 있었습니다. 예측 없이 결과만 봤으면 "맞았네" 로 넘어갔을 자리입니다. 자세한 것은 아래 §S3 에 적었습니다.

### 결과 요약

예측 **11 항목 전부 적중**했습니다. 그와 별개로 문서를 고쳐야 할 발견 셋이 나왔습니다.

| # | 실측 | 예측 적중 | 결과 |
|---|---|---|---|
| S1 | 라우트 트리 판정 (경유지 vs 목적지) | 2/2 | 문서 §3 서술과 일치 |
| S2 | 라벨 부재 시 부정 매처 통과 | 4/4 | 문서 §심화 학습 함정 재현 |
| S3 | 회피법 `=~ .+` | 4/4 | (c)(d) 만 차단, 정상 케이스 유지 |
| S4 | 억제 미발동 (실환경 20건) | 1/1 | `alertname` 불일치로 전부 active |
| S5 | 억제 발동 + 도착 순서 무관 | 2/2 | 후착 critical 이 선착 warning 을 소급 억제 |
| S6 | `group_wait` 분할 발송 | — | **미실시**. 별도 세션으로 이관 |

| # | 발견 | 문서 영향 |
|---|---|---|
| F1 | `amtool check config` 는 존재하지 않는 명령 | §7·치트시트 수정 완료 |
| F2 | 빈 값은 `namespace=""` 로 인용해야 파싱됨 | §심화 학습 추가 완료 |
| F3 | kube-prometheus 기본 억제는 사실상 발동하지 않음 | §6 두 단락 추가 완료 |

## 환경

살아 있는 스택을 그대로 썼습니다. 새로 띄운 것은 없습니다.

| 대상 | 실측값 |
|---|---|
| 클러스터 | kind-k8s-lab (노드 3대, kindest/node:v1.35.0) |
| Alertmanager | v0.33.1, `mon-lab` 네임스페이스, Operator 관리 |
| 클러스터 모드 | `disabled` · peers 0 (1대 배포) |
| Prometheus | v3.13.1-distroless |
| 발화 중인 알림 | 20건 (실측 시점) |
| 리시버 | `"null"` — kube-prometheus 기본값, 아무 데도 발송 안 함 |

살아 있는 설정은 Operator 가 `/etc/alertmanager/config_out/alertmanager.env.yaml` 에 씁니다. 1차 자료는 그 파일이 아니라 **API `/api/v2/status` 의 `config.original`** 입니다. Secret 은 Operator 가 다시 쓰기 전 상태일 수 있지만 API 는 지금 메모리에 로드된 것을 그대로 보여 줍니다.

실습용 설정 파일: `infra/k8s/alertmanager/routing-lab.yml` (클러스터에 적용하지 않는 `amtool` 판정 전용)

### 파드에 파일을 넣는 법

`kubectl cp` 가 실패합니다. 파드 루트FS 가 읽기 전용이라 tar 가 못 씁니다.

```
tar: can't remove old file t1.yml: Read-only file system
```

쓰기 가능한 경로는 `/alertmanager` 와 `/dev/shm` 둘뿐이었습니다. tar 를 거치지 않는 stdin 파이프로 우회합니다.

```bash
cat infra/k8s/alertmanager/routing-lab.yml | kubectl --context kind-k8s-lab \
  exec -i -n mon-lab alertmanager-kube-prom-kube-prometheus-alertmanager-0 \
  -c alertmanager -- sh -c 'cat > /alertmanager/lab.yml'
```

`amtool` 은 로컬 맥에 없고 파드 안에만 있습니다. 그래서 파일과 도구를 같은 곳에 둬야 합니다.

## S1. 라우트 트리 판정 — 경유지와 목적지는 다르다

**대상 트리** (살아 있는 kube-prometheus 기본값)

```
.
└── default-route  receiver: null
    └── {alertname="Watchdog"}  receiver: null
```

**예측**

| 알림 | 예측 경로 |
|---|---|
| `alertname=Watchdog` | 자식 라우트에 걸림 |
| `alertname=KubePodCrashLooping namespace=default severity=warning` | 자식에 안 걸려 최상위로 되돌아감 |

**실제** — 둘 다 `null` 출력. 리시버 이름이 같아 출력만으로는 구분되지 않았고, 트리 그림으로 경로를 확인했습니다.

**해석** — 예측 2/2 적중. 문서 §3 의 "부모 라우트에는 걸리지만 그 서브라우트 중 어느 것에도 걸리지 않으면 부모 라우트의 리시버로 되돌아갑니다" 가 실물로 확인됩니다.

여기서 얻은 실무 감각이 하나 있습니다. **리시버 이름이 같으면 `routes test` 출력만으로 경로를 알 수 없습니다.** kube-prometheus 가 둘 다 `null` 로 두었기 때문에 `amtool config routes` 로 트리를 함께 봐야 했습니다. 진단할 때 두 명령을 짝으로 쓰는 이유입니다.

`continue` 의 형제 탐색은 **확인하지 못했습니다.** 이 트리는 자식이 하나뿐이라 훑을 형제가 없습니다. 문서 §3 에 보강한 "형제가 없는 자리에 `continue` 를 달면 아무 일도 일어나지 않는다" 가 이 상황입니다.

## S2. 라벨 부재 시 부정 매처 통과

**시험 트리**

```yaml
route:
  receiver: "root"
  routes:
    - receiver: "has-namespace"
      matchers:
        - "namespace != default"
```

작성자의 의도는 "default 네임스페이스가 아닌 알림을 받자" 입니다.

**예측과 실제**

| | 알림 | 예측 | 실제 | 일치 |
|---|---|---|---|---|
| (a) | `namespace=kube-system` | has-namespace | has-namespace | O |
| (b) | `namespace=default` | root | root | O |
| (c) | namespace 라벨 없음 | has-namespace | has-namespace | O |
| (d) | `namespace=""` | has-namespace | has-namespace | O |

**해석** — 예측 4/4 적중. (c) 가 이 실측의 핵심입니다. 알림에 `namespace` 라벨이 **아예 없는데도** `namespace != default` 를 통과했습니다. 의도는 "default 가 아닌 네임스페이스" 였는데 실제로는 "네임스페이스를 알 수 없는 알림" 까지 받습니다.

(c) 와 (d) 가 같은 결과라는 점이 공식 문서의 `Semantically, a missing label and a label with an empty value are the same thing` 을 실증합니다.

이것이 실환경에서 위험한 이유는 지금 발화 중인 알림이 증명합니다. `TargetDown` 과 `Watchdog` 이 `namespace` 라벨 없이 발화 중이므로, 이 트리를 실제로 쓰면 둘 다 `has-namespace` 로 흘러듭니다.

### F2 — 빈 값은 인용해야 합니다

(d) 를 `namespace=` 로 처음 넣었을 때 결과는 `has-namespace` 로 예측과 같았지만 경고가 떴습니다.

```
level=WARN msg="Alertmanager is moving to a new parser for labels and matchers,
and this input is incompatible..." input="namespace=" origin=cli
err="end of input: expected label value" suggestion="namespace=\"\""
```

UTF-8 매처 파서가 입력을 거부하고 구식 파서로 폴백한 것입니다. **결과가 우연히 같아도 근거로 쓸 수 없습니다** — 파서가 입력을 제대로 읽은 결과가 아니기 때문입니다. `namespace=""` 로 다시 넣으니 경고 없이 같은 결과가 나왔고, 그제서야 (d) 를 근거로 삼았습니다.

예측을 먼저 적어 두지 않았다면 "예측대로 나왔다" 로 넘어갔을 자리입니다.

## S3. 회피법 — 존재 확인 매처

**바꾼 매처**

```yaml
matchers:
  - "namespace =~ .+"        # 라벨이 있고 값이 비어 있지 않을 것
  - "namespace != default"
```

**예측과 실제**

| | 앞선 결과 | 예측 | 실제 | 일치 |
|---|---|---|---|---|
| (a) | has-namespace | has-namespace (유지) | has-namespace | O |
| (b) | root | root (유지) | root | O |
| (c) | has-namespace | **root (변경)** | root | O |
| (d) | has-namespace | **root (변경)** | root | O |

**실제 트리 출력**

```
{namespace=~".+",namespace!="default"}  receiver: has-namespace
```

**해석** — 예측 4/4 적중. `.+` 는 "한 글자 이상" 이고 매처 정규식은 양쪽 끝이 앵커링되므로 빈 값(글자 0개)은 만족하지 못합니다. 함정만 막고 정상 케이스는 그대로입니다.

부수 확인 — 한 라우트에 매처를 여럿 걸면 **AND** 로 묶입니다. 트리 출력에서 쉼표로 이어진 형태로 드러납니다.

### F1 — `check config` 는 없는 명령입니다

문서 §7 과 치트시트가 `amtool check config alertmanager.yml` 로 적고 있었는데 실패했습니다.

```
amtool: error: expected command but got "check"
```

v0.33.1 의 하위 명령은 `check-config` 하이픈 한 단어입니다. 처음에는 버전 차이로 추정했으나 상류 소스를 확인하니 `checkCmd = app.Command("check-config", checkConfigHelp)` 로 **한 단어만 등록돼 있고 별칭이 없습니다.** 즉 원문(v0.25.0 기준)의 오기이지 버전이 올라가며 바뀐 것이 아닙니다.

통과하면 파싱된 항목을 세어 보여 줍니다. **내가 쓴 개수와 세어진 개수가 다르면 그것이 오타 신호입니다.**

```
Checking '/alertmanager/lab.yml'  SUCCESS
Found:
 - global config
 - route
 - 0 inhibit rules
 - 2 receivers
 - 0 templates
```

## S4. 억제 미발동 — 실환경 20건

**살아 있는 억제 규칙** (kube-prometheus 기본값)

```yaml
- source_matchers: [severity="critical"]
  target_matchers: [severity=~"warning|info"]
  equal: [namespace, alertname]
```

**대상** — `kube-system` 에 critical 하나와 warning 여럿이 동시 발화 중이었습니다.

| alertname | severity | namespace |
|---|---|---|
| `etcdInsufficientMembers` | critical | kube-system |
| `etcdMembersDown` | warning | kube-system |
| `KubeProxyInstanceUnreachable` | warning | kube-system |
| `KubeSchedulerInstanceUnreachable` | warning | kube-system |

**예측** — 하나도 억제되지 않습니다. `namespace` 는 같지만 `equal` 에 든 `alertname` 이 전부 다르기 때문입니다.

**실제** — 20건 전부 `state=active`, `inhibitedBy=[]`.

**해석** — 예측 적중.

### F3 — 기본 억제 규칙은 사실상 발동하지 않습니다

이 실측이 드러낸 것은 규칙의 **겨냥 범위**입니다. Alertmanager 는 알림 사이의 인과를 이해하지 못하고 라벨 값만 비교합니다. `equal` 에 `alertname` 이 들어 있으면 **이름이 같고 severity 만 다른** 알림만 걸립니다.

즉 이 기본 규칙은 다단계 임계값(디스크 90% warning / 98% critical)을 겨냥합니다. 그런데 kube-prometheus 가 함께 배포하는 기본 룰들은 그 패턴을 거의 쓰지 않습니다. `etcdInsufficientMembers` 와 `etcdMembersDown` 은 사람이 보기엔 상하위지만 이름이 달라 못 잡습니다.

**"kube-prometheus 를 깔았으니 억제 규칙이 알림 폭풍을 정리해 주겠지" 라는 믿음이 여기서 깨집니다.** 겨냥하는 상황과 기본 룰이 만들어 내는 상황이 어긋나 있습니다. 상하위 장애를 정리하려면 `alertname` 을 빼고 공통 라벨로 묶는 규칙을 직접 추가해야 합니다.

## S5. 억제 발동 + 도착 순서 무관

**설계** — `alertname` 과 `namespace` 를 같게 두고 `severity` 만 다른 알림 두 건을 주입합니다. 순서를 **뒤집어서** warning 을 먼저, critical 을 나중에 넣습니다. 순서 무관을 보려면 그쪽이 강한 검증입니다.

```bash
amtool alert add --alertmanager.url http://localhost:9093 \
  alertname=DiskUsageHigh namespace=lab severity=warning \
  --end="$(date -u -v+5M '+%Y-%m-%dT%H:%M:%SZ')"
```

**예측** — warning 이 `suppressed` 가 되고 `inhibitedBy` 에 critical 의 ID 가 채워집니다. 순서는 무관하며 판정 기준은 발송 시점에 critical 이 살아 있느냐입니다.

**실제**

```
① warning 주입 직후
warning   state=active       inhibitedBy=[]

② critical 주입 후
critical  state=active       inhibitedBy=[]
warning   state=suppressed   inhibitedBy=['1589e8d59d45cd3c']
```

**해석** — 예측 2/2 적중. 세 가지가 확인됩니다.

첫째, `alertname` 이 일치하니 억제가 걸립니다. S4 와 같은 규칙인데 결과가 갈린 유일한 차이가 `alertname` 입니다.

둘째, **후착 critical 이 선착 warning 을 소급해서 억제했습니다.** 문서 §6 의 "critical 알림이 warning 알림보다 늦게 도착해도 마찬가지입니다" 가 실증됩니다. 억제가 도착 시점이 아니라 **notify 단계**에서 판정되기 때문입니다.

셋째, source 인 critical 자신은 억제되지 않았습니다. `severity="critical"` 은 target 매처 `warning|info` 에 걸리지 않습니다.

## S6. group_wait 분할 발송 — 미실시

이 항목은 **실시하지 못했습니다.** 이유를 적습니다.

관측하려면 두 가지가 필요합니다. 발송이 눈에 보여야 하고(살아 있는 Alertmanager 는 리시버가 `null`), 타이머가 짧아야 합니다(기본 `group_wait: 30s` / `group_interval: 5m` 이라 셋째 알림을 보려면 5분 대기).

Spring 앱 메트릭으로 하려던 계획도 막혔습니다. 앱은 맥의 `localhost:8092` 에 있고 mon-lab Prometheus 는 kind 안에 있어 서로 못 봅니다. 앱을 클러스터에 올리면 되지만 그것은 이미지 빌드 → `kind load` → Deployment → ServiceMonitor 로 이어지는 별도 작업입니다.

그래서 **별도 세션으로 이관했습니다.** 그 세션의 산출물이 `PrometheusRule` 과 `AlertmanagerConfig` 이며, 거기서 타이머를 줄이고 MailHog 로 받으면 이 항목이 자연스럽게 완결됩니다.

이번 세션에서 Spring 앱은 기동까지만 확인했습니다 — `/actuator/prometheus` 가 233개 메트릭을 내보내고 `http_server_requests` 가 살아 있어 오류율 룰의 재료가 됩니다.

## 문서 반영

발견 셋을 정본 문서에 반영했습니다.

| 발견 | 반영 위치 |
|---|---|
| F1 `check-config` | §7 코드블록·설명, 결정 치트시트 |
| F2 빈 값 인용 | §심화 학습 |
| F3 `equal` 겨냥 범위 | §6 두 단락, 치트시트, 체크리스트 |

세션 중 나온 이해의 빈틈도 함께 메웠습니다.

| 빈틈 | 반영 위치 |
|---|---|
| 어느 키가 Prometheus 설정이고 어느 것이 Alertmanager 설정인지 | §1 신설 소절 "설정 파일이 둘로 갈립니다" |
| 라벨 누락을 매처에서 막기 vs 룰에서 채우기 | 같은 소절 |
| 가십 프로토콜이 무엇인지 | §9 |
| 가십·로드밸런서의 흐름이 글로만 있어 안 잡힘 | §9 SVG 신설 |

## 다음에 볼 것

- `continue` 형제 탐색 실측 — 이번 트리는 자식이 하나뿐이라 확인하지 못했습니다
- `group_wait` 분할 발송 (S6) — 별도 세션
- `AlertmanagerConfig` CR 적용 — 선언이 Operator 를 거쳐 트리에 병합되는 경로. Alertmanager CR 의 `alertmanagerConfigSelector` 가 CR 을 줍는지 먼저 확인해야 합니다
