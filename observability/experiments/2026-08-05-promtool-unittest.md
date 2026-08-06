# 알림 룰 단위 테스트 실습 — 05-02 원문 계산 검증

2026-08-05 · 《Mastering Prometheus》 5장 뒷부분(Unit-testing alerting rules) Phase 3 실습 기록입니다.

정본 문서: `~/runners-high/write/06_observability/book/mastering_prometheus/05-02.견고한 알림과 룰 단위 테스트.md`

같은 날 앞서 진행한 [Alertmanager 라우팅·억제 실습](2026-08-05-alertmanager-routing.md)의 뒷절입니다.

## 규약 준수 여부

**예측 → 실제 → 해석** 을 전 항목에서 지켰습니다. 예측 4항목 전부 적중했고, 그 덕에 **원문의 계산 오류를 잡았습니다.**

예측을 먼저 적지 않았다면 이 발견은 없었습니다. 첫 실행이 `SUCCESS` 였으므로 "문서대로구나" 로 넘어갔을 것입니다. 예측값(8분)과 문서값(9분)이 어긋난다는 것을 미리 적어 두었기에, 통과했는데도 "아직 못 가렸다" 고 판단해 단언을 좁혀 갈 수 있었습니다.

### 결과 요약

| # | 실측 | 예측 적중 | 결과 |
|---|---|---|---|
| S1 | `last_over_time` 발화 시점 | 2/2 | 4분 참 → 9분 발화 |
| S2 | 결측 한 번은 조건을 참으로 만들지 않음 | 1/1 | server2 는 발화하지 않음 |
| S3 | `max_over_time` 발화 시점 | 1/1 | **8분 참 → 13분 발화** (원문은 9분·14분) |

| # | 발견 | 문서 영향 |
|---|---|---|
| F1 | 원문의 발화 시점 계산이 한 칸 밀림 | 본문 163행 정정·§심화 학습 추가 완료 |
| F2 | 두 함수의 지연 차는 5분이 아니라 4분 | 본문 정정 완료 |
| F3 | 타임라인 SVG 가 원문 값 기준이었음 | 실측값으로 재작성 완료 |

## 환경

| 대상 | 실측값 |
|---|---|
| 클러스터 | kind-k8s-lab |
| `promtool` | v3.13.1 |
| 실행 위치 | `mon-lab` 네임스페이스에 띄운 일회성 파드 |

실습 파일: `infra/k8s/prometheus/rules-unittest-lab/` (`test-rule.yaml` · `unit-test.yaml`)

### 왜 별도 파드를 띄웠는가

Operator 가 배포한 Prometheus 파드에서 바로 돌리려 했으나 두 번 막혔습니다.

**첫째, 루트 파일시스템이 읽기 전용입니다.**

```
FAILED: opening test directory: mkdir /tmp/test_storage1072005293: read-only file system
```

`promtool test rules` 는 임시 TSDB 를 만들어 시계열을 적재하므로 쓰기 가능한 경로가 필요합니다.

**둘째, `TMPDIR` 로 우회할 수 없습니다.**

```
exec: "env": executable file not found in $PATH
```

이미지가 `v3.13.1-distroless` 라 셸도 `env` 도 없습니다. `kubectl exec` 로 환경변수를 주입할 방법이 없었습니다.

그래서 **같은 버전의 비-distroless 이미지**로 파드를 하나 띄웠습니다. `quay.io/prometheus/prometheus:v3.13.1` 에는 셸이 있어 stdin 파이프로 파일을 넣고 `promtool` 을 실행할 수 있습니다.

```bash
kubectl run promtool-lab -n mon-lab \
  --image=quay.io/prometheus/prometheus:v3.13.1 --restart=Never \
  --command -- sleep 3600

cat unit-test.yaml | kubectl exec -i -n mon-lab promtool-lab -- \
  sh -c 'cat > /tmp/lab/unit-test.yaml'

kubectl exec -n mon-lab promtool-lab -- \
  sh -c 'cd /tmp/lab && promtool test rules unit-test.yaml'
```

`cd` 가 필요한 이유는 `unit-test.yaml` 의 `rule_files` 가 상대 경로(`./test-rule.yaml`)이기 때문입니다.

Alertmanager 파드는 셸이 있어 stdin 파이프만으로 됐지만(앞 기록 참조), Prometheus 쪽은 distroless 라 한 겹 더 막혔습니다. **운영 이미지의 공격 표면 축소가 실습 도구 실행과 충돌하는 지점**입니다.

## 입력 시계열

문서 예제를 그대로 썼습니다.

```yaml
input_series:
  - series: 'probe_success{instance="server1", job="blackbox_ssh"}'
    values: "1 1 1 1 0+0x12"
  - series: 'probe_success{instance="server2", job="blackbox_ssh"}'
    values: "1 1 1 _ 1+0x12"
```

```
분:        0  1  2  3  4  5  6  7  8  9  10 ...
server1:   1  1  1  1  0  0  0  0  0  0  0  ...
server2:   1  1  1  _  1  1  1  1  1  1  1  ...
```

비교를 위해 룰을 하나 더 두었습니다. 조건과 `for` 는 같고 함수만 다릅니다.

| 룰 | 표현식 |
|---|---|
| `SSHDown` | `max_over_time(probe_success[5m]) != 1` |
| `SSHDownLast` | `last_over_time(probe_success[5m]) != 1` |

## S1. `last_over_time` 발화 시점

**예측** — 창의 마지막 값만 보므로 서버가 멈춘 4분에 곧바로 참이 되고, `for: 5m` 을 채워 9분에 발화합니다. 8분에는 아직 없어야 합니다.

**단언**

```yaml
- alertname: SSHDownLast
  eval_time: 8m
  exp_alerts: []
- alertname: SSHDownLast
  eval_time: 9m
  exp_alerts: [ ... instance: server1 ... ]
```

**실제** — 두 단언 모두 통과.

**해석** — 예측 적중. `for` 가 "조건이 참이 된 시점부터 5분" 을 세고 9분 시점에 발화한다는 것이 확인됩니다.

## S2. 결측 한 번은 조건을 참으로 만들지 않는다

**예측** — `server2` 는 3분에 결측이 하나 있을 뿐 값이 계속 `1` 이므로 조건 자체가 참이 되지 않습니다. 어느 룰에서도 발화하지 않습니다.

**실제** — 모든 단언에서 `server2` 가 나타나지 않았습니다.

**해석** — 예측 적중. `_over_time` 함수가 결측을 견디는 쪽으로 동작하므로 결측 하나가 `probe_success` 를 실패로 바꾸지 않습니다.

문서 §"스크레이프 실패가 `for` 를 지운다" 가 말하는 보호가 여기서 반대 방향으로도 확인됩니다. 결측은 알림을 **끄지도** 않고 **켜지도** 않습니다.

## S3. `max_over_time` 발화 시점 — 원문이 틀렸습니다

**원문 주장** (본문 163행)

> `max_over_time` 이 9분이 되어서야 `0` 을 돌려주기 시작하고 룰의 `for` 기간이 5분이므로 14분이 됩니다.

**예측** — 손으로 센 값은 8분 참 → 13분 발화였습니다. 원문과 한 칸 어긋납니다.

**1차 실행** — 문서 값(9분 발화 없음 · 14분 발화)을 단언으로 걸었더니 `SUCCESS`.

이것만으로는 가려지지 않습니다. 14분은 8분설이든 9분설이든 발화 후이기 때문입니다.

**2차 실행** — 13분에 "발화 없음"(문서 쪽)을 걸었습니다.

```
FAILED:
  alertname: SSHDown, time: 13m,
      exp:[],
      got:[ Labels:{alertname="SSHDown", instance="server1", ...} ]
```

13분에 이미 발화 중이었습니다. **원문의 14분이 틀렸습니다.**

**3차 실행** — 12분 발화 없음 + 13분 발화로 바꾸니 `SUCCESS`. 첫 발화가 13분으로 확정됩니다.

### 원인 — 첫 샘플이 0분에 놓인다

공식 문서는 `input_series` 의 샘플이 `start_timestamp` 부터 놓인다고 적습니다. 즉 **첫 값이 0분**입니다. `"1 1 1 1 0+0x12"` 는 0·1·2·3분이 `1` 이고 4분부터 `0` 입니다.

원문의 "처음 4분 동안 성공" 을 1~4분으로 읽으면 모든 시점이 한 칸씩 밀립니다. 문서 §심화 학습이 이미 정정한 **확장 표기법 오류(첫 항 누락)와 같은 뿌리**입니다 — 원저자가 샘플 인덱스를 1부터 센 것으로 보입니다.

범위 벡터 `[5m]` 은 `(t-5m, t]` 구간을 봅니다. 마지막 `1` 이 3분에 있으므로 창이 3분을 벗어나는 첫 시점은 8분입니다.

| 시점 | 창 `(t-5m, t]` | 최댓값 | `!= 1` |
|---|---|---|---|
| 7분 | 3·4·5·6·7분 = `1 0 0 0 0` | 1 | 거짓 |
| 8분 | 4·5·6·7·8분 = `0 0 0 0 0` | 0 | **참** |

`for: 5m` 을 더해 13분에 발화합니다.

### 두 함수의 지연 차는 4분

| 함수 | 참 | 발화 | 서버 정지(4분) 대비 |
|---|---|---|---|
| `last_over_time` | 4분 | 9분 | 5분 (`for` 만큼) |
| `max_over_time` | 8분 | 13분 | 9분 |

문서 §심화 학습이 "`max_over_time` 때문에 발화가 5분 늦어지는" 이라 적었으나 실측은 **4분**입니다. 창 길이(5분)가 아니라 마지막 `1` 이 창을 벗어나기까지 걸리는 시간(4분)이 지연의 크기입니다.

## 문서 반영

| 발견 | 반영 위치 |
|---|---|
| F1 계산이 한 칸 밀림 | 본문 163행에 정정 표시, §심화 학습에 근거·창 계산표·원인 |
| F2 지연 차 4분 | §심화 학습 비교표, `last_over_time` 권장 단락 |
| F3 SVG | `_assets/05-02.alert-timeline.svg` 재작성 — 두 함수를 나란히 두어 발화 시점 차이를 막대로 표현 |

Phase 1 질의응답에서 드러난 빈틈도 함께 메웠습니다.

| 빈틈 | 반영 위치 |
|---|---|
| 5분 lookback 이 있는데 왜 빈 결과인가 (stale 마커) | §"스크레이프 실패가 `for` 를 지운다" |
| 빈 결과를 무엇으로 참·거짓 판정하는가 (비교 연산자가 걸러냄) | 같은 절 |

## 이어서 — PrometheusRule CRD 로 실제 발화시키기 (2026-08-06)

위 실습은 `promtool` 로 **계산만** 확인했습니다. 룰을 클러스터에 넣지 않았으므로 "선언 → Operator 반영 → 실제 발화" 경로는 밟지 않았습니다. 그 빈 구간을 채운 회차입니다.

실습 파일: `infra/k8s/prometheus/prometheusrule-lab.yaml` (실습 후 삭제)

### 설계

같은 조건에 `for` 만 달리한 룰 셋을 두었습니다. 조건은 `default/liveness-fail` 파드의 재시작 카운터 증가로, 26일째 CrashLoopBackOff 라 항상 참입니다.

| 룰 | `for` | 의도 |
|---|---|---|
| `LabRestartNoFor` | 없음 | 대조군 |
| `LabRestartWithFor` | 2m | pending 구간을 눈으로 |
| `LabRestartShortFor` | 10s | 평가 주기(30s)보다 짧음 |

Operator 조건은 미리 조회해 확인했습니다. `ruleSelector` 가 `release: kube-prom` 이고 `ruleNamespaceSelector` 가 비어 있어, 라벨만 맞추면 스택 스펙을 건드리지 않고 CR 을 추가할 수 있었습니다.

### 예측과 실제

**예측** — 적용 직후 `NoFor` 만 firing, 1분 뒤 `ShortFor` 까지, 3분 뒤 셋 다. `ShortFor` 는 평가 주기보다 짧아 `NoFor` 와 사실상 같은 시점에 발화.

**실제** — 예측대로였습니다.

```
[적용 2분 후]  FIRING (2)  PENDING (1)      [4분 후]  FIRING (3)
  NoFor      firing                            NoFor      firing
  WithFor    pending      ────────→            WithFor    firing
  ShortFor   firing                            ShortFor   firing
```

### activeAt 이 셋 다 같습니다

삭제 직전 기록입니다.

```
LabRestartNoFor      for=0    s  state=firing  activeAt=2026-08-06T00:58:36
LabRestartWithFor    for=120  s  state=firing  activeAt=2026-08-06T00:58:36
LabRestartShortFor   for=10   s  state=firing  activeAt=2026-08-06T00:58:36
```

**`activeAt` 은 조건이 참이 된 시각이지 발화한 시각이 아닙니다.** 셋이 같은 조건을 보므로 값이 일치하고, 그 뒤 `for` 만 발화 시점을 갈랐습니다. UI 에서 pending 과 firing 을 구분해 보지 않으면 이 차이가 드러나지 않습니다.

### `for` 가 평가 주기보다 짧으면

`ShortFor`(10s)가 `NoFor`(없음)와 끝까지 같은 상태였습니다. `evaluationInterval` 이 30s 이므로 조건이 참이 된 뒤 **다음 평가에서 곧바로** firing 이 됩니다. `10s` 든 `25s` 든 결과가 같아, 잠깐 튀는 값을 거르려는 목적이 달성되지 않습니다.

문서 §2 가 "5분마다 평가하는 룰에 `for: 3m` 을 걸면 그 3분은 아무 일도 하지 않는다" 고 적은 그 함정입니다.

### 적용·삭제 경로

```
kubectl apply → CR 생성
             → Operator 가 rulefiles Secret 갱신
             → config-reloader 가 Reload 트리거
             → Prometheus 가 룰 로드 (UI 에 파일 경로가 보임)
             → 조건 충족 → pending → firing
```

삭제는 반대 방향으로 같은 경로를 지납니다. CR 은 즉시 사라지지만 Prometheus API 에서 빠지기까지 몇십 초가 걸립니다. 그 사이 `kubectl get` 은 NotFound 인데 `/api/v1/rules` 에는 남아 있어, **두 곳의 상태가 잠시 어긋납니다.**

### 부수 발견

**Prometheus UI 는 룰 그룹을 페이지네이션합니다.** 그룹이 22개라 1·2·3 페이지로 나뉘고 `lab-for-demo` 는 2페이지에 있었습니다. 1페이지만 보고 "적용이 안 됐다" 고 판단하기 쉽습니다. 검색창(`?search=` 쿼리)을 쓰면 바로 걸립니다.

**`keep_firing_for` 를 차트에서 발견했습니다.** kube-prometheus 기본 룰 상당수가 `for` 바로 아래 이 절을 답니다. `for` 가 발화 전 대기라면 이것은 조건이 거짓이 된 **뒤에도** firing 을 유지하는 시간이며, flapping 을 막습니다. 책에 없는 개념이라 정본 문서 §2 에 보강했습니다.

**기본 룰이 저자의 경험칙을 따릅니다.** etcd 룰에서 같은 `alertname` 에 warning 은 `10m`, critical 은 `5m` 이 걸려 있습니다. 그리고 이 쌍은 `alertname` 이 같으므로 kube-prometheus 기본 억제 규칙(`equal: [namespace, alertname]`)이 실제로 걸립니다 — 앞 회차에서 `etcdInsufficientMembers` 와 `etcdMembersDown` 이 억제되지 않았던 것은 이름이 달랐기 때문입니다.

### 문서 반영

| 발견 | 반영 위치 |
|---|---|
| inactive·pending·firing 정의 없음 | §2 소절 신설. `inactive` 는 공식 용어가 아니라 UI·API 표현이라는 점도 명시 |
| `keep_firing_for` | §2 소절 신설, 각주 1개 |
| `for` 가 평가 주기보다 짧을 때 | §2 한 줄 보강 |
| §관련 문서가 "용어집이 pending·firing 을 정의한다" 고 안내하나 실제로 없음 | 깨진 참조 수정 |

## 참고 — 외부 차트 읽기

`infra/k8s/_chart/` 에 kube-prometheus-stack 차트를 받아 두었습니다. Git 에서 제외되며(`.gitignore` 의 `**/_chart/*`, README 만 예외) 받는 법은 그 README 에 있습니다.

기본 알림 룰은 `templates/prometheus/rules-1.14/` 에 주제별로 나뉘어 있고 `for` 가 290건 쓰여 있습니다. 다만 Helm 문법(`{{ dig "etcdMembersDown" "for" "20m" .Values.customRules }}`)에 싸여 있어, 값만 보려면 클러스터에서 렌더링된 결과를 받는 편이 낫습니다.

설치 시 기본값에서 바꾼 값은 `infra/k8s/kube-prom-values.yaml` 에 두었습니다. 재현에 필요하므로 이쪽은 저장소에 포함합니다.

## 다음에 볼 것

- `promtool check rules` 와 `test rules` 의 출력 차이 — 아직 `test rules` 만 돌렸습니다
- `stale` 마커를 `input_series` 에 넣어 `_` 와 동작이 어떻게 갈리는지
- `PrometheusRule` CRD 의 `spec.groups` 를 떼어 내 `promtool` 에 먹이기 — 12장(CI 파이프라인)과 이어집니다
- `keep_firing_for` 를 실제로 걸어 flapping 이 덮이는지 관측
