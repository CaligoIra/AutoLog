# 오토로그 (AutoLog)

**계기판 연동 자동 운행기록부** — Pleos Connect(Android Automotive) 차량용 앱

시동을 걸고 기어를 바꾸기만 하면 운행마다 출발·도착 시각과 주행 전·후 계기판 누적거리가 저절로 기록됩니다.
법인·개인사업자 차량이 국세청 「업무용승용차 운행기록부」를 쓸 때 손으로 계기판 숫자를 옮겨 적던 일을 없애는 것이 목표입니다.

> 현대자동차그룹 차량용 앱 개발 공모전 출품작 · 현재 프로토타입 단계

## 지금 동작하는 것

| 기능 | 설명 |
|---|---|
| 운행 자동 시작·종료 | 기어가 P에서 다른 단으로 바뀌면 운행 시작, 다시 P가 되면 운행 종료 |
| 계기판 거리 기록 | 시작·종료 순간의 누적거리(km)를 저장하고 주행거리를 계산 |
| 시동부터 백그라운드 기록 | 차량 화면 시스템이 켜지면 기록 서비스가 저절로 시작되어, 앱 화면을 열지 않아도 기록 |
| 끊김 없는 기록 | 운행 도중 앱이 종료되거나 시스템이 재시작되어도 같은 운행을 이어서 기록 |
| 잘못된 값 거르기 | 부팅 직후 들어오는 0 km, 출발값보다 작은 값은 무시하고, 움직이지 않은 운행(0 km)은 남기지 않음 |
| 목적지 연결 | 내비 목적지 도착 이벤트를 받아 운행의 도착지로 기록 (연결만 되어 있고 아직 시험 전) |

## 만들 예정

- 음성으로 운행 목적 남기기
- 업무용 / 출퇴근 / 비업무 구분
- 국세청 운행기록부 서식으로 엑셀 내보내기

## 동작 방식

```
Pleos Vehicle SDK ─ Odometer·Gear ─▶ VehicleMonitor ─┐
                                                      ├─▶ TripRecorderService ─▶ TripRecorder ─▶ Room DB ─▶ MainViewModel ─▶ 화면
Pleos NaviHelper ── 목적지 도착 ───▶ NaviMonitor ────┘     (포그라운드 서비스)      (운행 구간 판단)
```

- **TripRecorderService**: 부팅(`BOOT_COMPLETED`)이나 앱 실행 때 시작되는 포그라운드 서비스(`specialUse`, `START_STICKY`). 기어·계기판 값을 받아 TripRecorder에 넘깁니다.
- **TripRecorder**: 기어 상태로 운행 구간을 자릅니다. 진행 중인 운행도 DB에 `endedAt`이 비어 있는 행으로 저장하므로, 앱이 다시 켜지면 그 행에서 이어갑니다.
- **화면**: 기록하지 않고 DB를 보여주기만 합니다. 운전 중에도 볼 수 있도록 입력 없이 한눈에 읽히게 구성했습니다(`distractionOptimized`).

## 사용한 Pleos SDK

| SDK | 버전 | 사용 기능 | 권한 |
|---|---|---|---|
| Vehicle | 2.3.2 | `Odometer`(누적 주행거리, km), `Gear`(기어 선택 상태) | `pleos.car.permission.CAR_DRIVE` |
| NaviHelper | 3.0.5 | `onDestinationArrived`(목적지 도착) | — |

## 폴더 구조

```
app/src/main/java/com/autolog/app/
├── AutoLogApp.kt               앱 전체에서 하나씩 쓰는 DB·차량 연결
├── MainActivity.kt             화면 진입, 기록 서비스 시작
├── MainViewModel.kt            DB의 운행 기록을 화면에 전달
├── recorder/TripRecorderService.kt   포그라운드 기록 서비스 + 부팅 리시버
├── trip/                       Trip(운행 1건), TripDao·DB, TripRecorder(운행 구간 판단)
├── vehicle/VehicleMonitor.kt   Pleos Vehicle SDK 래퍼
├── navi/NaviMonitor.kt         Pleos NaviHelper 래퍼
└── ui/AutoLogScreen.kt         Compose 화면
```

## 빌드

- Android Studio 2026.2 (내장 JDK) · AGP 9.5.0 · Kotlin 2.4.21 · Gradle 9.8.2
- compileSdk 37 · targetSdk 34 · minSdk 28
- Pleos SDK는 `settings.gradle.kts`에 등록된 Pleos Maven 저장소에서 받습니다(인증 불필요).

```bash
./gradlew assembleDebug
```

결과물: `app/build/outputs/apk/debug/app-debug.apk`

## 에뮬레이터에서 시험하기

1. Pleos Playground 안내에 따라 Pleos Connect 에뮬레이터(API 34)를 설치하고 실행합니다.
2. 앱을 설치하고 실행합니다.
   ```bash
   adb install -r -g app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.autolog.app/.MainActivity
   ```
3. Virtual Console을 엽니다.
   ```bash
   adb forward tcp:8088 tcp:8088
   ```
   브라우저에서 http://127.0.0.1:8088/ → **Emulation Controls** → **Motion**
4. **Gear Selection**에서 D를 누르고, **Odometer** 값을 올린 뒤 P를 누르면 오른쪽 목록에 운행 1건이 추가됩니다.

참고: 에뮬레이터는 재부팅하면 계기판 값이 0으로 돌아가고, 현재값보다 작은 값은 받지 않습니다(HTTP 400).
