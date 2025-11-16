앱 내에서 `NotificationHistoryService` 참조가 해결되지 않는 오류가 발생하여, 해당 서비스 및 관련 기능들을 제거하고 개인화된 알림 기능으로 대체하는 작업을 완료했습니다. 또한, 개인 맞춤 알림 기능을 직접 확인하고 테스트할 수 있는 섹션을 `TestPageActivity`에 추가했으며, **출발 전 맞춤형 알림 메시지를 `MainActivity` 상단 바에서 바로 확인할 수 있도록 했고, 이 알림 버튼을 누르면 알림 내역을 볼 수 있는 페이지로 이동하도록 변경했습니다.**

**주요 변경 및 해결 내용:**

1.  **`NotificationHistoryService` 및 `NotificationHistoryActivity` 제거**:
    *   `app/src/main/kotlin/com/OnTime/ontime/ui/NotificationHistoryActivity.kt` 파일이 삭제되었습니다.
    *   `app/src/main/kotlin/com/OnTime/ontime/service/NotificationHistoryService.kt` 파일이 삭제되었습니다.
    *   `AndroidManifest.xml`에서 `NotificationHistoryActivity`에 대한 선언이 제거되었습니다.
    *   `MainActivity.kt`의 `TopAppBar`에서 `NotificationHistoryActivity`로 이동하는 `IconButton`이 제거되었습니다.
    *   `CalendarViewModel.kt` 내 `testRAGSystem()` 함수에서 `NotificationHistoryService`에 대한 모든 참조(인스턴스화 및 `saveNotification` 호출)가 제거되었습니다. 이로써 `Unresolved reference: NotificationHistoryService` 오류가 해결되었습니다.

2.  **개인화된 알림 기능 통합**:
    *   **`NotificationBuilder.kt` 수정**:
        *   `generateNotificationMessage` 함수가 이제 Google Maps의 예상 이동 시간(`travelTime`)과 모델이 예측한(`predictedActualTravelTime`) 또는 실제 추적된(`actualCalculatedTravelTime`) 이동 시간을 비교하여 사용자에게 맞춤화된 메시지를 생성합니다.
        *   사용자 선호도(`UserPreferences`)에 따른 알림 메시지 톤(친근한, 격식 있는, 재치 있는 등)을 반영하여 메시지를 생성합니다.
    *   **`NotificationScheduler.kt` 수정**:
        *   `NotificationBuilder`와 `SettingsRepository`를 의존성으로 받아 알림 메시지 생성 및 사용자 환경설정 접근에 사용합니다.
        *   `scheduleNotification` 함수는 이제 `예상 이동 시간`, `예측된 실제 이동 시간`, 그리고 `날씨 정보`를 매개변수로 받으며, 생성된 알림 메시지(`String?`)를 반환하도록 변경되었습니다.
        *   반환된 메시지를 `NotificationReceiver`로 전달하여 알림이 표시되도록 합니다.
    *   **`RouteCalculationWorker.kt` 수정**:
        *   `NotificationBuilder`, `SettingsRepository`, `WeatherRepository`, `LocationConverter`, 그리고 새로 생성된 `NotificationRepository`를 인스턴스화하여 사용합니다.
        *   모델 예측을 위한 **플레이스홀더** 로직이 포함되었습니다. `estimatedTravelTimeMinutes`에 무작위 편차(예: ±10%)를 주어 `predictedActualTravelTimeMinutes`를 시뮬레이션합니다. (향후 실제 모델 통합 시 이 부분을 모델 추론 결과로 대체하면 됩니다.)
        *   이벤트 목적지에 대한 날씨 정보를 가져와 알림 메시지 생성에 활용합니다.
        *   `notificationScheduler.scheduleNotification`을 호출하여 알림을 예약하고, 반환된 메시지와 기타 관련 정보를 사용하여 **`NotificationRecord`를 생성한 후 `NotificationRepository`를 통해 Firebase에 저장**합니다.

3.  **알림 내역 페이지 (`NotificationListPageActivity.kt`) 및 통합**:
    *   **`NotificationRecord` 데이터 클래스 생성**: `app/src/main/kotlin/com/OnTime/ontime/data/models/NotificationRecord.kt`에 알림 기록의 구조를 정의했습니다.
    *   **`NotificationRepository` 생성**: `app/src/main/kotlin/com/OnTime/ontime/data/repositories/NotificationRepository.kt`에 `NotificationRecord`를 Firebase Firestore에 저장하고 가져오는 로직을 구현했습니다.
    *   **`NotificationListPageActivity.kt` 생성**: `app/src/main/kotlin/com/OnTime/ontime/ui/NotificationListPageActivity.kt`에 Firebase에서 알림 기록을 가져와 `LazyColumn`으로 표시하는 Composable 화면을 만들었습니다.
    *   **`AndroidManifest.xml` 업데이트**: `NotificationListPageActivity`를 선언했습니다.
    *   **`MainActivity.kt` 수정**: `TopAppBar`의 알림 아이콘(`Icons.Outlined.Notifications`) 클릭 시, `Toast` 메시지를 표시하는 대신 **`NotificationListPageActivity`로 이동**하도록 변경했습니다. (아이콘의 `contentDescription`도 "알림 내역"으로 변경)

4.  **`TestPageActivity.kt` 개선 및 오류 수정**:
    *   `kotlin.math.roundToInt` 확장 함수 관련 오류를 수정했습니다.
    *   `SettingsComponents.kt`에서 `Spacer(Modifier = Modifier.height(8.dp))`와 같이 잘못된 매개변수 이름을 사용한 부분을 수정했습니다.
    *   `TestPageActivity`의 모든 사용자 인터페이스 텍스트를 한국어로 번역했습니다.
    *   "개인 맞춤 알림 테스트" 섹션을 추가하여 `TestPageActivity`에서 직접 개인 맞춤 알림 메시지를 시뮬레이션하고 확인할 수 있도록 했습니다.
    *   자동 실제 이동 시간 계산 및 출발 감지 로직을 구현했으며, 현재 위치 주소 표시 기능과 페이지 스크롤 기능도 추가했습니다.

**개인 맞춤 알림 기능을 확인하는 방법:**

1.  **앱 실행**: Android 앱을 빌드하고 실행합니다.
2.  **메인 화면 확인**: `MainActivity`의 상단 바(TopAppBar)를 확인합니다.
3.  **알림 아이콘 클릭**: "설정" 아이콘 왼쪽에 있는 **알림 아이콘**(`Icons.Outlined.Notifications`)을 클릭합니다.
4.  **알림 내역 페이지 확인**: `NotificationListPageActivity`로 이동하며, Firebase에 저장된 개인 맞춤 알림 내역 목록을 볼 수 있습니다. (초기에는 비어있을 수 있습니다.)

**알림 내역을 Firebase에 생성하는 방법:**

*   **백그라운드 워커를 통해 자동 생성**: `RouteCalculationWorker`가 주기적으로 실행되면서 알림을 예약하고 Firebase에 `NotificationRecord`를 저장합니다. 앱이 백그라운드에서 실행될 때 알림이 예약되면 기록이 쌓이게 됩니다.
*   **`TestPageActivity`를 통해 수동 생성**:
    1.  `MainActivity`에서 "테스트 페이지로 이동" 버튼을 클릭합니다.
    2.  캘린더 이벤트를 선택하고 현재 위치 및 예상 이동 시간을 가져옵니다.
    3.  "여행 추적 시작" 버튼을 클릭하여 이동을 시뮬레이션하고 목적지에 도착하면 `실제 이동 시간`이 자동 계산됩니다.
    4.  **"Firebase에 이동 기록 저장" 버튼을 클릭**하여 이 `TravelLog` 데이터를 Firebase에 저장합니다. (현재 `TestPageActivity`에서 직접 `NotificationRecord`를 저장하는 기능은 없지만, `RouteCalculationWorker`의 로직은 `TravelLog` 생성이 아닌 `NotificationRecord` 생성을 포함합니다.)

**중요**: 현재 `RouteCalculationWorker`가 주기적으로 실행되어 알림을 예약하고 `NotificationRecord`를 Firebase에 저장하는 로직이 있습니다. 실제 알림이 예약되어야 `NotificationRecord`가 Firebase에 쌓일 것입니다.

이것으로 사용자님의 모든 요청과 보고된 오류에 대한 수정 및 기능 구현이 완료되었습니다. 추가적인 질문이나 변경 사항이 있으시면 언제든지 말씀해 주세요.