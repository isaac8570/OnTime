# OnTime - 비서형 AI 경로 및 알림 앱

## 프로젝트 구조

### 1. 프로젝트 레벨 및 기본 파일

```
OnTime (Root Project)
├── build.gradle.kts (project)  // 프로젝트 설정
├── build.gradle.kts (app)      // 앱 모듈 종속성 및 설정
├── AndroidManifest.xml         // 앱 컴포넌트 및 권한 정의
├── res
│   ├── values
│   │   ├── strings.xml         // 모든 문자열 리소스
│   │   └── colors.xml          // 디자인 색상 정의
└── app/src/main/kotlin/com/yourcompany/ontime
    └── ... (하위 Kotlin 패키지)
```

### 2. Kotlin 소스 코드 구조 (com/yourcompany/ontime)

```

```
app/src/main/kotlin/com/yourcompany/ontime
├── api (외부 서비스 통신)
│   ├── DirectionsService.kt   // Google Maps Directions API
│   ├── GeminiApiService.kt    // Gemini API (LLM 알림 문구 생성)
│   ├── PlacesApiService.kt    // Google Maps Places API (Autocomplete)
│   └── RetrofitClient.kt      // Retrofit 인스턴스 설정
│
├── data (데이터 모델 및 저장소)
│   ├── models
│   │   ├── CalendarEvent.kt   // 캘린더 일정 데이터 모델
│   │   └── UserPreferences.kt // 사용자 설정 (톤, 교통수단) 모델
│   └── repositories
│       ├── CalendarRepository.kt  // Google Calendar API 통신 로직
│       └── SettingsRepository.kt  // 사용자 설정 저장/로드 로직
│
├── service (백그라운드 작업 및 알림)
│   ├── RouteCalculationWorker.kt  // WorkManager: 경로 계산 및 알림 시간 예약 핵심 로직
│   ├── NotificationScheduler.kt   // AlarmManager/FCM 알림 예약 관리
│   ├── NotificationManager.kt     // Android 알림 채널 및 표시
│   ├── ReminderWorker.kt          // WorkManager: "출발했는지 묻는 반복" 알림 처리
│   └── AuthService.kt             // Firebase Authentication 처리
│
├── ui (사용자 인터페이스)
│   ├── MainActivity.kt            // 메인 화면 및 네비게이션 컨테이너
│   ├── CalendarListFragment.kt    // 일정 목록 표시 화면 (메인)
│   ├── EventDetailFragment.kt     // 경로 상세 계산 결과 화면
│   ├── SettingsActivity.kt        // 환경 설정 화면
│   ├── AuthActivity.kt            // 구글 로그인/인증 화면
│   └── viewmodel
│       └── CalendarViewModel.kt   // 일정 데이터 관리 ViewModel
│
└── util (공통 유틸리티)
    ├── Constants.kt           // API 키, 상수 정의
    ├── DateUtil.kt            // 날짜 및 시간 유틸리티
    ├── Logger.kt              // 커스텀 로깅
    └── LocationConverter.kt   // 주소-위경도 변환 헬퍼
```

## 구글 캘린더 - 경로 계산 및 알림 아이디어

https://developers.google.com/workspace/calendar/api/guides/overview?hl=ko

- 강하게 말하기/ 약한버전 - 알림을 보낼 때 LLM 인공지능 api 사용
- 문제 상황: 일정을 깜빡하거나, 시간 계산을 잘못해 약속에 늦는 일을 방지,
- 경로를 미리 계산해 예상시간을 알려주어 비서형 인공지능 기능
- 사용하는 것: google calender api, firebase 알림/소리/진동 사용, 출발했는지 묻는 반복
- 이동수단: 도보 / 자가 / 대중교통 선택할 수 있게
- Google Maps Platform - 10,000회 무료
- 앱 UI
    - 구글 사용자 계정 접근 권한
    - https://developers.google.com/maps?hl=ko

# 비서형 AI 경로 및 알림 앱 개발 로드맵

## 1. 프로젝트 개요 및 목표

- **앱 이름 (가칭):** 프롬프트 출발 (Prompt Depart) 또는 타임 마스터
- **핵심 문제:** 사용자가 일정을 잊거나, 예상치 못한 교통 상황으로 인해 약속 시간에 늦는 문제.
- **핵심 솔루션:** 구글 캘린더 일정에 기반하여, 예상 이동 시간과 실시간 교통 상황을 고려한 '최적 출발 시간'을 계산하고, LLM 기반의 맞춤 알림을 제공하는 비서형 인공지능 앱.
- **주요 기술:** Google Calendar API, Google Maps Directions API, Gemini API (LLM), Firebase.

## 2. 핵심 기능 및 기술 스택

| 기능 범주 | 상세 기능 | 필수 API / 기술 | 담당자 |
| --- | --- | --- | --- |
| **인증 및 계정** | 구글 계정으로 로그인 (Google Sign-In) | Firebase Authentication, Google Play Services |  |
| **일정 관리** | 캘린더 일정 읽기 및 동기화 | Google Calendar API |  |
| **경로 계산** | 출발/도착지 경로 및 소요 시간 계산 (대중교통/자가용/도보) | Google Maps Directions API |  |
| **위치 검색** | 출발지, 도착지 입력 시 자동 완성 기능 | Google Maps Places API (Autocomplete) |  |
| **스마트 알림** | 최적 출발 시간 계산 및 알림 발송 (사전 알림) | Firebase Cloud Messaging (FCM) |  |
| **LLM 기반 알림** | 사용자가 설정한 톤(강함/약함)에 따른 알림 문구 생성 | **Gemini API** (LLM) |  |
| **백그라운드 처리** | 일정 변경 감지, 알림 예약/실행 | Android WorkManager (백그라운드 작업) |  |

## 3. 개발 로드맵 (3단계)

### Phase 1: MVP (Minimum Viable Product) 구축

핵심 기능을 검증하고 사용성을 확보하는 단계입니다.

| 항목 | 상세 내용 |
| --- | --- |
| **A. 환경 설정** | Android 프로젝트 생성, Firebase 및 Google API 키 설정. |
| **B. 인증 및 캘린더** | Google 계정 로그인 구현, Google Calendar API 연동 및 일정 목록 표시. |
| **C. 기본 경로 계산** | 일정의 위치 정보를 기반으로 **대중교통** 경로만 계산 (Directions API 연동). |
| **D. 기본 알림** | 계산된 소요 시간을 역산하여 단순한 푸시 알림 발송 (FCM 및 Android 알림). |
| **E. UI/UX** | 깔끔하고 직관적인 일정 및 경로 표시 화면 디자인 (Material Design). |
| **MVP 결과** | 캘린더 연동, 대중교통 경로 계산, 단순 알림 기능이 작동하는 앱 출시. |

### Phase 2: 확장 및 스마트 기능 구현

사용자의 피드백을 반영하고 비서형 AI의 핵심 기능을 완성하는 단계입니다.

| 항목 | 상세 내용 |
| --- | --- |
| **A. 이동 수단 다양화** | `DRIVING` (자가용), `WALKING` (도보) 옵션 추가 및 사용자 선택 기능 구현. |
| **B. LLM 알림 통합** | **Gemini API**를 호출하여 '강한 버전' 또는 '약한 버전'의 알림 문구를 생성하고 알림에 적용. |
| **C. 출발 확인 루프** | 알림 발송 후 일정 시간 간격으로 "출발했는지 묻는 반복 알림" 구현. (예: "아직 출발 안 하셨어요! 10분 남았습니다.") |
| **D. 위치 자동 완성** | Google Maps Places API (Autocomplete)를 이용한 출발/도착지 입력 기능 개선. |
| **E. 백그라운드 최적화** | WorkManager를 사용하여 배터리 효율성을 고려하며 일정 동기화 및 알림 예약. |

### Phase 3: 고급 기능 및 개선

앱의 완성도를 높이고 개인화 기능을 추가합니다.

| 항목 | 상세 내용 |
| --- | --- |
| **A. 사용자 설정** | 알림 톤(강함/약함) 설정, 기본 이동 수단 설정, 알림 반복 주기 설정 기능. |
| **B. 교통 상황 예측** | Directions API 요청 시 `trafficModel` 옵션을 사용하여 운전 경로의 실시간 교통 예측 반영. |
| **C. 일정 편집 연동** | 앱 내에서 캘린더 일정 위치/시간 변경 후 구글 캘린더에 반영하는 기능 (선택 사항). |

## 4. LLM (Gemini API) 통합 전략

Gemini API는 사용자의 설정에 따라 맞춤화된 알림 메시지를 생성하는 데 사용됩니다.

1. **프롬프트 설계:** 알림 상황(예: "출발 시간 30분 전", "지각 위험"), 이동 수단, 목적지 정보를 프롬프트에 포함합니다.
2. **톤 조절:** 시스템 인스트럭션 또는 사용자 입력(변수)을 통해 원하는 톤을 명확히 정의합니다.

| 설정 톤 | LLM 시스템 인스트럭션 (예시) | 예시 출력 문구 |
| --- | --- | --- |
| **강한 버전** | "당신은 절대 지각을 허용하지 않는 단호하고 엄격한 비서입니다. 단답형으로 경고하십시오." | "경고! [XX역]까지 20분 남았습니다. 즉시 출발하세요." |
| **약한 버전** | "당신은 친절하고 부드러운 비서입니다. 격려하는 톤으로 알림을 보내세요." | "[XX역]으로 출발할 시간이에요. 부드럽게 준비하시고, 오늘도 즐거운 하루 보내세요!" |

### Gemini API 요청 예시 (의사 코드)

```javascript
// 알림 메시지 생성을 위한 LLM 호출
const prompt = `사용자의 다음 일정: ${일정_제목}. 출발해야 할 시간입니다. 이동 수단은 ${이동_수단}이고 목적지는 ${도착지}입니다. 이 상황에 맞는 알림 메시지를 생성해 주세요.`;

const systemPrompt = "당신은 사용자가 설정한 '강한 버전'의 비서 역할을 합니다. 단호하고 경고하는 톤으로 짧은 메시지를 생성하십시오.";

const payload = {
    contents: [{ parts: [{ text: prompt }] }],
    systemInstruction: { parts: [{ text: systemPrompt }] },
};
// ... API 호출 및 메시지 추출
```