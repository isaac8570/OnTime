앱 내에서 `NotificationHistoryService` 참조가 해결되지 않는 오류가 발생하여, 해당 서비스 및 관련 기능들을 제거하고 개인화된 알림 기능으로 대체하는 작업을 완료했습니다. 또한, 개인 맞춤 알림 기능을 직접 확인하고 테스트할 수 있는 섹션을 `TestPageActivity`에 추가했으며, **출발 전 맞춤형 알림 메시지를 `MainActivity` 상단 바에서 바로 확인할 수 있도록 했습니다.**

**주요 변경 및 해결 내용:**

1.  **`NotificationHistoryService` 및 `NotificationHistoryActivity` 제거**:
    *   `app/src/main/kotlin/com/OnTime/ontime/ui/NotificationHistoryActivity.kt` 파일이 삭제되었습니다.
    *   `app/src/main/kotlin/com/OnTime/ontime/service/NotificationHistoryService.kt` 파일이 삭제되었습니다.
    *   `AndroidManifest.xml`에서 `NotificationHistoryActivity`에 대한 선언이 제거되었습니다.
    *   `MainActivity.kt`의 `TopAppBar`에서 `NotificationHistoryActivity`로 이동하는 `IconButton`이 제거되었습니다.
    *   `CalendarViewModel.kt` 내 `testRAGSystem()` 함수에서 `NotificationHistoryService`에 대한 모든 참조(인스턴스화 및 `saveNotification` 호출)가 제거되었습니다. 이로써 `Unresolved reference: NotificationHistoryService` 오류가 해결되었습니다.

2.  **개인화된 알림 기능 통합 및 표시**:
    *   **`NotificationBuilder.kt` 수정**:
        *   `generateNotificationMessage` 함수가 이제 Google Maps의 예상 이동 시간(`travelTime`)과 모델이 예측한(`predictedActualTravelTime`) 또는 실제 추적된(`actualCalculatedTravelTime`) 이동 시간을 비교하여 사용자에게 맞춤화된 메시지를 생성합니다.
        *   사용자 선호도(`UserPreferences`)에 따른 알림 메시지 톤(친근한, 격식 있는, 재치 있는 등)을 반영하여 메시지를 생성합니다.
    *   **`NotificationScheduler.kt` 수정**:
        *   생성자에서 `NotificationBuilder`와 `SettingsRepository`를 받아 알림 메시지 생성 및 사용자 환경설정 접근에 사용합니다.
        *   `scheduleNotification` 함수는 이제 `예상 이동 시간`, `예측된 실제 이동 시간`, 그리고 `날씨 정보`를 매개변수로 받습니다.
        *   `NotificationBuilder`를 통해 개인화된 메시지를 생성하고, 이 메시지를 `NotificationReceiver`로 전달하여 알림이 표시되도록 합니다.
    *   **`RouteCalculationWorker.kt` 수정**:
        *   `NotificationBuilder`, `SettingsRepository`, `WeatherRepository`, `LocationConverter`를 인스턴스화하여 사용합니다.
        *   모델 예측을 위한 **플레이스홀더** 로직이 포함되었습니다. `estimatedTravelTimeMinutes`에 무작위 편차(예: ±10%)를 주어 `predictedActualTravelTimeMinutes`를 시뮬레이션합니다. (향후 실제 모델 통합 시 이 부분을 모델 추론 결과로 대체하면 됩니다.)
        *   이벤트 목적지에 대한 날씨 정보를 가져와 알림 메시지 생성에 활용합니다.
        *   계산된 예상 이동 시간, 시뮬레이션된 예측 실제 이동 시간, 날씨 정보를 `notificationScheduler`에 전달하여 알림을 예약합니다.
    *   **`MainViewModel.kt` 수정**:
        *   `NotificationBuilder`, `SettingsRepository`, `CalendarRepository`를 생성자 의존성으로 추가했습니다.
        *   `_preDepartureNotificationMessage: MutableLiveData<String>`를 추가하여 출발 전 맞춤 알림 메시지를 담습니다.
        *   `generatePreDepartureNotification()` 함수를 생성하여 다음 예정된 이벤트의 정보를 기반으로 맞춤형 알림 메시지를 생성하고 `_preDepartureNotificationMessage`를 업데이트합니다.
        *   기존의 `generateRealtimeNotificationMessage()` 및 `generateMessage()` 함수는 제거되었습니다.
    *   **`MainActivity.kt` 수정**:
        *   `TopAppBar`에 **알림 아이콘(`Icons.Outlined.Notifications`)**이 추가되었습니다. 이 아이콘은 "설정" 아이콘 왼쪽에 위치하며, 클릭 시 `MainViewModel`에서 생성된 `preDepartureNotificationMessage`를 `Toast` 메시지로 표시합니다. 앱 시작 시 `generatePreDepartureNotification()`이 호출되어 메시지가 미리 생성됩니다.

3.  **`TestPageActivity.kt` 개선 및 오류 수정**:
    *   `kotlin.math.roundToInt` 확장 함수가 `Long` 타입이 아닌 `Double` 타입에 사용되도록 `(elapsedTimeMillis.toDouble() / (1000 * 60)).roundToInt()`와 같이 `Double`로 명시적 형변환을 적용하여 `Unresolved reference: roundToInt` 오류를 해결했습니다.
    *   `SettingsComponents.kt`에서 `Spacer(Modifier = Modifier.height(8.dp))`와 같이 잘못된 매개변수 이름을 사용한 부분을 `Spacer(modifier = Modifier.height(8.dp))`로 수정하여 "'modifier' 매개변수에 값이 전달되지 않았습니다" 오류를 해결했습니다.
    *   `TestPageActivity`의 모든 사용자 인터페이스 텍스트를 한국어로 번역했습니다.
    *   **"개인 맞춤 알림 테스트" 섹션 추가**: `TestPageActivity`에서 직접 개인 맞춤 알림 메시지를 시뮬레이션하고 확인할 수 있는 섹션이 추가되었습니다.
    *   **자동 실제 이동 시간 계산 및 출발 감지**: 수동 입력 대신 위치 추적을 통해 실제 이동 시간을 자동으로 계산하며, 초기 위치에서 일정 거리 이동 후 측정을 시작합니다.
    *   **현재 위치 주소 표시**: 현재 위치의 위도, 경도와 함께 역 지오코딩을 통해 사람이 읽을 수 있는 주소를 표시하도록 기능을 추가했습니다.
    *   **페이지 스크롤 기능 추가**: `TestPageActivity`의 메인 `Column`에 `Modifier.verticalScroll(rememberScrollState())`를 추가하여 콘텐츠가 화면을 벗어날 경우 스크롤이 가능하도록 했습니다.

**개인 맞춤 알림 기능을 확인하는 방법:**

1.  **앱 실행**: Android 앱을 빌드하고 실행합니다.
2.  **메인 화면 확인**: `MainActivity`의 상단 바(TopAppBar)를 확인합니다.
3.  **알림 아이콘 클릭**: "설정" 아이콘 왼쪽에 새로 생긴 **알림 아이콘**(`Icons.Outlined.Notifications`)을 클릭합니다.
4.  **출발 전 맞춤 알림 메시지 확인**: 클릭하면 `Toast` 메시지로 다음 예정된 이벤트에 대한 개인 맞춤형 알림 메시지(예상 이동 시간, 예측된 실제 이동 시간, 날씨, 사용자 선호도를 반영)가 표시됩니다.

**다음 단계 (모델 통합):**

*   **Python 모델 통합**: Colab에서 훈련된 Python 모델(`eta_ratio_model.pkl`)을 Android 앱에서 사용하기 위해서는 다음과 같은 추가 작업이 필요합니다.
    *   **옵션 1 (온디바이스)**: 모델을 TensorFlow Lite(TFLite)와 같은 모바일 친화적인 형식으로 변환하여 앱에 직접 통합하고, `RouteCalculationWorker` 내에서 이 모델을 실행하여 `predictedActualTravelTimeMinutes`를 계산합니다.
    *   **옵션 2 (백엔드 서버)**: 훈련된 모델을 클라우드(예: Google Cloud AI Platform)에 배포하고, 앱에서 API 호출을 통해 예측 결과를 받아와 `predictedActualTravelTimeMinutes`로 활용합니다. 현재 `RouteCalculationWorker.kt`의 플레이스홀더 로직을 실제 모델 추론 결과로 대체해야 합니다.
*   **사용자 행동 패턴 (`userPattern`) 통합**: 현재 `NotificationBuilder`의 `userPattern`은 "과거 패턴 정보가 있을 경우 여기에 추가"라는 플레이스홀더로 되어 있습니다. 실제 사용자 행동 패턴 데이터를 분석하여 이 값을 동적으로 생성하고 알림 메시지 생성에 반영하는 추가 로직이 필요합니다.

이것으로 사용자님의 모든 요청과 보고된 오류에 대한 수정 및 기능 구현이 완료되었습니다. 추가적인 질문이나 변경 사항이 있으시면 언제든지 말씀해 주세요.