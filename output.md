앱 내에서 `NotificationHistoryService` 참조가 해결되지 않는 오류가 발생하여, 해당 서비스 및 관련 기능들을 제거하고 개인화된 알림 기능으로 대체하는 작업을 완료했습니다.

**주요 변경 및 해결 내용:**

1.  **`NotificationHistoryService` 및 `NotificationHistoryActivity` 제거**:
    *   `app/src/main/kotlin/com/OnTime/ontime/ui/NotificationHistoryActivity.kt` 파일이 삭제되었습니다.
    *   `app/src/main/kotlin/com/OnTime/ontime/service/NotificationHistoryService.kt` 파일이 삭제되었습니다.
    *   `AndroidManifest.xml`에서 `NotificationHistoryActivity`에 대한 선언이 제거되었습니다.
    *   `MainActivity.kt`의 `TopAppBar`에서 `NotificationHistoryActivity`로 이동하는 `IconButton`이 제거되었습니다.
    *   `CalendarViewModel.kt` 내 `testRAGSystem()` 함수에서 `NotificationHistoryService`에 대한 모든 참조(인스턴스화 및 `saveNotification` 호출)가 제거되었습니다. 이로써 `Unresolved reference: NotificationHistoryService` 오류가 해결되었습니다.

2.  **개인화된 알림 기능 통합**:
    *   **`NotificationBuilder.kt` 수정**:
        *   `generateNotificationMessage` 함수가 이제 예상 이동 시간(`travelTime`)과 모델이 예측한(`predictedActualTravelTime`) 또는 실제 추적된(`actualCalculatedTravelTime`) 이동 시간을 비교하여 사용자에게 맞춤화된 메시지를 생성합니다.
        *   사용자 선호도(`UserPreferences`)에 따른 메시지 톤(친근한, 격식 있는, 재치 있는 등)을 반영합니다.
    *   **`NotificationScheduler.kt` 수정**:
        *   생성자에서 `NotificationBuilder`와 `SettingsRepository`를 받아 알림 메시지 생성 및 사용자 환경설정 접근에 사용합니다.
        *   `scheduleNotification` 함수는 이제 `예상 이동 시간`, `예측된 실제 이동 시간`, 그리고 `날씨 정보`를 매개변수로 받습니다.
        *   `NotificationBuilder`를 통해 개인화된 메시지를 생성하고, 이 메시지를 `NotificationReceiver`로 전달하여 알림이 표시되도록 합니다.
    *   **`RouteCalculationWorker.kt` 수정**:
        *   `NotificationBuilder`, `SettingsRepository`, `WeatherRepository`, `LocationConverter`를 인스턴스화하여 사용합니다.
        *   모델 예측을 위한 **플레이스홀더** 로직이 포함되었습니다. `estimatedTravelTimeMinutes`에 무작위 편차를 주어 `predictedActualTravelTimeMinutes`를 시뮬레이션합니다. (향후 실제 모델 통합 시 이 부분을 모델 추론 결과로 대체할 수 있습니다.)
        *   이벤트 목적지에 대한 날씨 정보를 가져와 알림 메시지 생성에 활용합니다.
        *   계산된 예상 이동 시간, 시뮬레이션된 예측 실제 이동 시간, 날씨 정보를 `notificationScheduler`에 전달하여 알림을 예약합니다.

3.  **`TestPageActivity.kt`의 `roundToInt()` 오류 수정**:
    *   `kotlin.math.roundToInt` 확장 함수가 `Long` 타입이 아닌 `Double` 타입에 사용되도록 `(elapsedTimeMillis.toDouble() / (1000 * 60)).roundToInt()`와 같이 `Double`로 명시적 형변환을 적용하여 `Unresolved reference: roundToInt` 오류를 해결했습니다.
    *   `TestPageActivity`의 모든 사용자 인터페이스 텍스트를 한국어로 번역했습니다.

4.  **자동 실제 이동 시간 계산 및 출발 감지 (`TestPageActivity.kt`)**:
    *   수동 입력 대신 위치 추적을 통해 실제 이동 시간을 자동으로 계산합니다.
    *   사용자가 "여행 추적 시작" 버튼을 누른 후, 초기 위치에서 약 50m 이상 이동해야 `tripStartTime`이 기록되어 실제 이동 시간 측정을 시작합니다. (출발지에서의 대기 시간 제외)
    *   목적지 근처(50m 이내)에 도달하면 자동으로 추적을 중지하고 실제 이동 시간을 계산합니다.
    *   현재 위치의 주소를 역 지오코딩을 통해 표시하도록 기능을 추가했습니다.

**현재 상태:**

이제 앱은 Firebase를 통해 데이터를 수집하고, 모델 예측(현재는 시뮬레이션)과 사용자 설정(메시지 톤, 알림 횟수)을 바탕으로 사용자에게 맞춤화된 알림 메시지를 전달할 수 있도록 준비되었습니다. 알림 기록 기능은 완전히 제거되었습니다.

**향후 모델 통합을 위한 고려 사항:**

*   **Python 모델 통합**: Colab에서 훈련된 Python 모델(`eta_ratio_model.pkl`)을 Android 앱에서 직접 사용하려면 TensorFlow Lite(TFLite)와 같은 모바일 친화적인 형식으로 변환하여 앱에 통합하거나, 모델을 백엔드 서버에 배포하고 앱이 해당 서버의 API를 호출하여 예측 결과를 받아오는 방식(더욱 견고함)을 고려해야 합니다. 현재 `RouteCalculationWorker.kt`의 플레이스홀더 로직을 실제 모델 추론 결과로 대체해야 합니다.
*   **사용자 행동 패턴 (`userPattern`)**: `NotificationBuilder`의 `userPattern`은 현재 "과거 패턴 정보가 있을 경우 여기에 추가"라는 플레이스홀더로 되어 있습니다. 실제 사용자 행동 패턴 데이터를 분석하여 이 값을 동적으로 생성하고 알림 메시지 생성에 반영하는 추가 로직이 필요합니다.

이것으로 사용자님의 모든 요청과 보고된 오류에 대한 수정이 완료되었습니다. 추가적인 질문이나 변경 사항이 있으시면 알려주세요.