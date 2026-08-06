# `_chart/` — 외부 Helm 차트 읽기용 사본

Git 에서 제외됩니다(`.gitignore` 의 `**/_chart/`). 차트 원문을 읽어 보려고 받아 두는 자리이며, 여기 있는 파일을 고쳐도 클러스터에는 아무 영향이 없습니다.

저장소에 넣지 않는 이유는 셋입니다. 남의 코드라 버전을 올릴 때마다 통째로 갈아야 하고, 크기가 커서(kube-prometheus-stack 하나가 7MB) 히스토리를 채우며, 이미 클러스터에 적용된 것이 1차 자료라 사본은 어긋날 수 있습니다.

## 받는 법

```bash
cd infra/k8s/_chart
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update prometheus-community
helm pull prometheus-community/kube-prometheus-stack --version 87.15.1 --untar
```

버전은 실제 설치본과 맞춥니다.

```bash
helm list -A --kube-context kind-k8s-lab
```

## 받아 둔 차트

| 차트 | 버전 | 릴리스 | 네임스페이스 |
|---|---|---|---|
| `kube-prometheus-stack` | 87.15.1 | `kube-prom` | `mon-lab` |

## 어디를 보면 되는가

`kube-prometheus-stack` 기준입니다.

| 경로 | 내용 |
|---|---|
| `values.yaml` | 설정 가능한 전체 값과 기본값 |
| `templates/prometheus/rules-1.14/` | 기본 알림 룰·기록 룰. 주제별 파일 |
| `templates/prometheus/prometheus.yaml` | Prometheus CR 템플릿 |
| `templates/alertmanager/alertmanager.yaml` | Alertmanager CR 템플릿 |

템플릿에는 Helm 문법(`{{ ... }}`)이 섞여 있어 읽기 불편합니다. 값이 박힌 결과를 보려면 클러스터에서 직접 받는 편이 낫습니다.

```bash
# 실제 적용된 매니페스트 전체
helm get manifest kube-prom -n mon-lab --kube-context kind-k8s-lab

# 알림 룰만
kubectl --context kind-k8s-lab get prometheusrule -n mon-lab -o yaml
```

## 새 차트를 추가할 때

같은 자리에 `helm pull ... --untar` 로 풀고 위 표에 한 줄 더합니다. 디렉토리 이름은 차트 이름 그대로 둡니다.

## 관련

설치 시 사용한 values 는 재현에 필요하므로 `_chart/` 가 아니라 저장소에 둡니다.

```bash
helm get values kube-prom -n mon-lab --kube-context kind-k8s-lab > ../kube-prom-values.yaml
```
