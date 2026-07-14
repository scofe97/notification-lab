# 기반 기술 — Undertow 실측 노트 (Tomcat과 비교)

이 문서는 특정 UC가 아니라 **프로젝트 전역 기반**인 내장 웹서버 Undertow를 손으로 확인하는 노트입니다. 여기서는 이 프로젝트에서 실제로 비교·실측하는 절차만 둡니다.

- **상태**: 이 프로젝트는 Undertow로 기동됨 (`build.gradle`에서 tomcat exclude + starter-undertow)

---

## 1. 지금 Undertow로 뜨는지 확인 (부팅 로그 한 줄)

```bash
# bootRun 로그에서
grep "Undertow started" <부팅로그>
#   → "Undertow started on port 8092" 가 나오면 Tomcat 아님
#   Tomcat이면 "Tomcat started on port(s): ..." 로 뜬다
```
> 이 프로젝트 실측(2026-07-09): `Undertow started on port 8092 (http)` 확인됨.

---

## 2. Tomcat과 나란히 띄워 직접 비교 (교체가 의존성 한 줄임을 체감)

```bash
# build.gradle에서 아래를 잠깐 스왑
#   - spring-boot-starter-undertow 를 빼고
#   - spring-boot-starter-web 의 tomcat exclude 를 지우면 → Tomcat으로 뜬다
# 코드는 한 줄도 안 바꾸고 부팅 로그가 "Tomcat started"로 바뀌는지 본다.
# 확인 후 반드시 원복 (이 프로젝트 기준은 Undertow).
```
> 핵심 관찰: `@RestController`·`@KafkaListener` 코드는 그대로인데 서버만 바뀐다 = 서블릿 스펙 위에서 돌기 때문. 이걸 눈으로 보면 "왜 교체가 싼가"가 체감된다.

---

## 3. 스레드 모델 차이 관찰 (스레드 이름·개수)

```bash
# 앱 부팅 후 PID 확인
jps | grep Notification

jstack <pid> | grep -iE 'XNIO|undertow|http-nio|catalina' | sort | uniq -c
#   Undertow → "XNIO-1 I/O-…", "XNIO-1 task-…" 스레드
#   Tomcat  → "http-nio-8092-…", "catalina-…" 스레드
# 스레드 개수·이름 패턴이 두 서버의 아키텍처 차이를 그대로 드러낸다.
```

기본값 검증 (튜닝하지 않았으므로 기본값이 뜹니다):
- `io-threads` = CPU 코어 × 2 → `jstack`의 `XNIO … I/O` 스레드 수를 세어 코어×2와 맞는지 확인
- `worker-threads` = io-threads × 8

---

## 4. 메모리 footprint 대략 비교 (idle 기준)

```bash
jcmd <pid> GC.heap_info        # 힙 사용량
# Undertow ↔ Tomcat 각각 idle 부팅 후 비교하면 코어 라이브러리 무게 차이가 보인다.
```

---

## 5. 확인 기록 (직접 채움)

> 연구 결론과 실측으로 새로 안 것을 아래에 덧붙입니다.

- [ ] Undertow 스레드 이름(XNIO) 확인:
- [ ] io-threads가 CPU 코어×2와 맞는가:
- [ ] Tomcat과 idle 힙 비교 결과 (N MB 차이):
- 막힌 점:
- 새로 안 것:
