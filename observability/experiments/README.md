# experiments/ — 재현법·증거·원인 판단 기록 (3단계)

3단계 관측 스터디의 결과물 폴더입니다.

- 담을 것: 관측 UC(01~12)별 실험 기록 — 재현 조건 → metric·log·trace 증거 → 원인 판단. 예: `01-normal-baseline.md`, `02-poison-message-dlt.md`.
- 채우는 시점: [ROADMAP 3단계](../../ROADMAP.md) 착수 시. 실험 목록은 [관측 시나리오와 운영 절차](../docs/02-scenarios-and-operations.md).

## 기록 규약

**예측 → 실제 → 해석** 셋을 지킵니다. 예측 칸을 비운 채 실행하지 않습니다. 결과가 예측과 같아도 그 경로가 올바른지는 따로 봅니다 — 08-05 기록의 `namespace=` 파서 폴백이 그 사례입니다.

## 기록 목록

| 날짜 | 기록 | 다룬 것 |
|---|---|---|
| 2026-07-28 | [Prometheus 스택 기동](2026-07-28-prometheus-stack.md) | 스택 구성과 기동 실패 원인 |
| 2026-08-04 | [서비스 디스커버리와 relabeling](2026-08-04-sd-relabeling.md) | 04-01 문서 블록 대조, relabeling 두 시점 |
| 2026-08-05 | [Alertmanager 라우팅·억제](2026-08-05-alertmanager-routing.md) | 05-01 대조. 라벨 부재 매처 함정·억제 `equal` 겨냥 범위·도착 순서 무관 |
