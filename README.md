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
| 잠깐 정차는 한 운행 | 신호 대기·드라이브스루처럼 P에 넣었다가 정한 시간(기본 5분) 안에 다시 출발하면 같은 운행으로 이어 기록(출발 시각·주행 전 거리 유지). 첫 화면 '잠깐 정차 이어 기록' 카드에서 켜기·끄기와 1·3·5·10분을 고름(주차 중에만). 출발하는 순간 목적 질문은 멈춤. 목적을 답한 뒤 출발하면 그곳에서 볼일을 본 것으로 보고 새 운행으로 나눔 |
| 잘못된 값 거르기 | 부팅 직후 들어오는 0 km, 출발값보다 작은 값은 무시하고, 움직이지 않은 운행(0 km)은 남기지 않음 |
| 목적지 연결 | 내비 목적지 도착 이벤트를 받아 운행의 도착지로 기록 (연결만 되어 있고 아직 시험 전) |
| 운행 목적 음성 기록 | 기어를 P로 두면 "이번 운행 목적을 말씀해 주세요"라고 묻고, 답을 운행기록부 비고(목적)로 저장. 못 알아들으면 한 번 더 묻고, 놓친 운행은 주차 중 「목적 말하기」 버튼으로 다시 물음. "그만"·"나중에"라고 하면 묻기를 멈춤 |
| 목적 고치기 | 주차 중 「고치기」로 잘못 알아들은 목적을 키보드로 고치거나 「말로 하기」로 다시 말함. 목적이 빈 운행은 「직접 입력」도 가능 |
| 목적 몰아서 답하기 | 「목적 없는 운행 n건 · 차례로 답하기」를 누르면 "11월 3일 오전 10시 16분에 출발한 8킬로미터 운행의 목적을…"처럼 오래된 운행부터 차례로 물음. 출발하면 멈춤 |
| 출퇴근 자동 판정 | 운행 시작·종료 때 차 위치를 내비 즐겨찾기의 집·회사와 비교해(300 m 이내) 집→회사는 '출근', 회사→집은 '퇴근' 운행으로 묻지 않고 기록 |
| 업무 구분 | 운행기록부 서식대로 출퇴근 / 일반업무 / 비업무로 나눔. 출퇴근이 아니면 목적 답으로 정하고("개인 …"이라고 답하면 비업무), 주차 중 화면에서 바꿀 수 있음 |
| 업무사용비율 | 올해 총주행거리, 업무용 사용거리(출퇴근 + 일반업무)와 업무사용비율을 화면 위에 표시. 구분이 안 된 운행은 업무용에 넣지 않음 |
| 운행기록부 보기 | 주차 중 올해 운행을 국세청 서식과 같은 칸 순서(사용일자, 사용자, 주행 전·후 계기판 거리, 주행거리, 출·퇴근용, 일반 업무용, 비고)의 표로 보여주고, 합계·업무사용비율과 비어 있는 칸(목적 없음·구분 안 됨)을 알려줌. 표의 구분 칸(‘?’)과 비고 칸(‘목적 없음’)을 누르면 그 자리에서 고칠 수 있음(누를 수 있는 칸은 옅은 바탕). 기록 목록은 날짜별로 묶어 그날 거리를 함께 표시 |
| 엑셀 내보내기 | 운행기록부 화면에서 고른 해의 기록을 국세청 서식과 같은 칸·병합·머리글의 엑셀(.xlsx)로 만들어 다운로드/AutoLog 폴더에 저장. 주행거리·합계·업무사용비율은 수식으로 넣어 엑셀에서 숫자를 고쳐도 따라가고, 가로 A4 한 장 너비로 인쇄되며 쪽마다 머리글이 되풀이됨 |
| 기록부 정보(선택) | 상호, 사업자등록번호, 차종, 자동차등록번호를 한 번 적어 두면 엑셀 서식 위쪽 칸에 채움. 적지 않으면 빈칸으로 둠 |
| 운전자 목록·운행별 선택 | 「기록부 정보」에 운전자(성명·부서)를 여러 명 등록해 두고, 운행기록부 표의 사용자 칸을 눌러 운행마다 고름. 고르지 않은 운행은 맨 위의 기본 운전자로 채움 |

음성은 Pleos TextToSpeech·SpeechToText SDK가 아직 준비 중이라 Android 표준 `TextToSpeech`·`SpeechRecognizer`로 만들었고, `VoiceIO` 인터페이스 뒤에 두어 SDK가 나오면 구현만 바꿉니다.
에뮬레이터에서는 표준 `SpeechRecognizer`가 Pleos 인식 서비스(`ai.umos.ambientai`)에 연결되는 것까지 확인했습니다. 다만 에뮬레이터에는 마이크 입력이 없고, 내장 음성 합성 엔진은 표준 `TextToSpeech`로 찾을 수 없어(엔진 등록에 DEFAULT 카테고리가 없음) 소리 없이 화면 글자로 진행됩니다.

## 만들 예정

- 내보낸 엑셀을 이메일로 보내기. 차량에는 파일 저장 창이나 파일을 넘겨받을 앱이 없어(에뮬레이터에서 확인) 지금은 다운로드 폴더에 바로 저장합니다.

## 동작 방식

```
Pleos Vehicle SDK ─ Odometer·Gear ─▶ VehicleMonitor ─┐
                                                      ├─▶ TripRecorderService ─▶ TripRecorder ─▶ Room DB ─▶ MainViewModel ─▶ 화면
Pleos NaviHelper ── 목적지 도착 ───▶ NaviMonitor ────┘     (포그라운드 서비스)      (운행 구간 판단)
```

- **TripRecorderService**: 부팅(`BOOT_COMPLETED`)이나 앱 실행 때 시작되는 포그라운드 서비스(`specialUse`, `START_STICKY`). 기어·계기판 값을 받아 TripRecorder에 넘깁니다.
  Android 14는 앱 화면이 보이지 않을 때 마이크 유형으로 바꾸는 것을 막으므로, 앱 화면이 보일 때 `microphone` 유형을 더합니다.
- **TripRecorder**: 기어 상태로 운행 구간을 자릅니다. 진행 중인 운행도 DB에 `endedAt`이 비어 있는 행으로 저장하므로, 앱이 다시 켜지면 그 행에서 이어갑니다.
  운전 중에는 열린 운행을 메모리에 들고 있어 계기판 값이 바뀔 때마다 DB를 읽지 않습니다. P 뒤 설정한 시간(기본 5분, `ResumeSettingsStore`) 안에 다시 출발하면 목적을 아직 답하지 않은 운행을 이어 갑니다. 이어 기록을 끄면 P마다 운행을 나눕니다.
- **TripClassifier**: 운행 시작·종료 때 NaviMonitor에 차가 집·회사 근처인지 묻습니다. 출발지를 모르면 앞 운행의 도착지를 출발지로 보고, 집→회사·회사→집이면 출퇴근으로 정합니다.
- **PurposeAsker**: 운행이 끝나면(P) 목적을 음성으로 묻고 답을 그 운행의 `purpose`에 저장하며, 답으로 일반업무·비업무를 정합니다. 출퇴근 운행은 묻지 않고 기록했다고만 알립니다. 묻는 동안에도 기록은 멈추지 않습니다.
- **화면**: 기록하지 않고 DB를 보여주기만 합니다. 운전 중에도 볼 수 있도록 한눈에 읽히게 구성했고(`distractionOptimized`), 목적 말하기·고치기, 업무 구분 선택, 운행기록부 보기는 주차(P) 중에만 눌리고 출발하면 닫힙니다.

## 사용한 Pleos SDK

| SDK | 버전 | 사용 기능 | 권한 |
|---|---|---|---|
| Vehicle | 2.3.2 | `Odometer`(누적 주행거리, km), `Gear`(기어 선택 상태) | `pleos.car.permission.CAR_DRIVE` |
| NaviHelper | 3.0.5 | `onDestinationArrived`(목적지 도착), `getBookmarkInfo`(즐겨찾기 집·회사), `getCurrentLocationInfo`(현재 위치) | `pleos.car.permission.NAVI_ROUTE` |

음성 질문·인식은 Pleos SDK 공개 전까지 Android 표준 API를 쓰며 `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE` 권한이 필요합니다(마이크를 거절해도 기록은 계속됨).

## 폴더 구조

```
app/src/main/java/com/autolog/app/
├── AutoLogApp.kt               앱 전체에서 하나씩 쓰는 DB·차량 연결
├── MainActivity.kt             화면 진입, 기록 서비스 시작
├── MainViewModel.kt            DB의 운행 기록을 화면에 전달
├── recorder/TripRecorderService.kt   포그라운드 기록 서비스 + 부팅 리시버
├── trip/                       Trip(운행 1건), TripDao·DB, TripRecorder(운행 구간 판단),
│                               TripType(업무 구분·합계), TripClassifier(출퇴근 판정),
│                               ResumeSettings(잠깐 정차 이어 기록 켜기·시간)
├── vehicle/VehicleMonitor.kt   Pleos Vehicle SDK 래퍼
├── navi/NaviMonitor.kt         Pleos NaviHelper 래퍼(도착 이벤트, 집·회사 근처인지 판단)
├── voice/                      VoiceIO(음성 입출력), AndroidVoiceIO(표준 API 구현), PurposeAsker(주차 후 목적 묻기)
├── export/                     LogbookInfo(기록부 정보·운전자 목록), LogbookExport(서식대로 엑셀 쓰기·다운로드 저장),
│                               XlsxSheet(라이브러리 없이 .xlsx 만들기)
├── ui/AutoLogScreen.kt         Compose 화면(상태, 날짜별 기록, 목적 고치기 대화상자)
└── ui/LogbookSheet.kt          국세청 운행기록부 서식 모양의 표, 기록부 정보·운전자 선택, 엑셀로 내보내기
```

`app/src/debug`에는 에뮬레이터에서 시험할 수 있도록 adb로 보낸 문장을 음성 인식 결과 대신, adb로 보낸 장소를 내비 현재 위치 대신 쓰는 디버그 전용 구현이 있습니다(릴리스 빌드에는 들어가지 않음).

## 빌드

- Android Studio 2026.2 (내장 JDK) · AGP 9.5.0 · Kotlin 2.4.21 · Gradle 9.8.2
- compileSdk 37 · targetSdk 34 · minSdk 28
- Pleos SDK는 `settings.gradle.kts`에 등록된 Pleos Maven 저장소에서 받습니다(인증 불필요).

```bash
./gradlew assembleDebug
```

결과물: `app/build/outputs/apk/debug/app-debug.apk`

### 릴리스 APK

공모전 제출용 서명 APK는 [`apk/CaligoIra_오토로그_release.apk`](apk/CaligoIra_오토로그_release.apk)에 있습니다(release, v2 서명, Pleos Connect 에뮬레이터에서 설치·기록 확인).

```bash
adb install -g apk/CaligoIra_오토로그_release.apk
```

직접 만들 때는 서명 정보를 저장소 밖에 두고 프로젝트 루트의 `keystore.properties`(git에 올리지 않음)에 적습니다. 이 파일이 없으면 서명 없는 release APK가 만들어집니다.

```properties
storeFile=<키 파일 경로>.jks
storePassword=...
keyAlias=...
keyPassword=...
```

```bash
./gradlew assembleRelease
```

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
   조작 사이에 몇 초씩 기다려 주세요. 너무 빨리 바꾸면 기어보다 계기판 값이 먼저 도착해 움직이지 않은 운행으로 처리됩니다.
5. 목적 음성 기록은 디버그 빌드에서, P를 누르기 전에 답을 미리 보내 두면 시험할 수 있습니다.
   ```bash
   adb shell am broadcast --user 10 -a com.autolog.app.debug.ANSWER -p com.autolog.app --es text "'A사 납품 미팅'"
   ```
   여러 번 보내면 보낸 순서대로 하나씩 쓰므로, 「차례로 답하기」도 답을 미리 몇 개 보내 두고 시험할 수 있습니다.
6. 출퇴근 판정은 먼저 에뮬레이터 내비 앱에서 즐겨찾기 집·회사를 등록해 두고 시험합니다.
   에뮬레이터 내비의 현재 위치는 한곳에 고정되어 있어(`adb emu geo fix`도 따르지 않음), 디버그 빌드에서는 D를 누르기 전에 출발 장소를, P를 누르기 전에 도착 장소를 보냅니다.
   ```bash
   adb shell am broadcast --user 10 -a com.autolog.app.debug.PLACE -p com.autolog.app --es place HOME
   adb shell am broadcast --user 10 -a com.autolog.app.debug.PLACE -p com.autolog.app --es place OFFICE
   ```
   HOME→OFFICE면 묻지 않고 '출근' 운행으로 기록됩니다. 보낸 장소는 다음 위치 확인 한 번에만 쓰입니다.
7. 운행기록부 보기 → 「엑셀로 내보내기」로 만든 파일은 다음처럼 꺼낼 수 있습니다(`<id>`는 첫 명령으로 확인).
   ```bash
   adb shell content query --user 10 --uri content://media/external/downloads --projection _id:_display_name
   adb exec-out content read --user 10 --uri content://media/external/downloads/<id> > logbook.xlsx
   ```

참고: 에뮬레이터는 재부팅하면 계기판 값이 0으로 돌아가고, 현재값보다 작은 값은 받지 않습니다(HTTP 400).
