package com.OnTime.ontime.ui

import android.os.Build // 추가: API 레벨 확인을 위해 필요
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalTime // API Level 26+
import kotlin.math.roundToInt // roundToInt() 함수를 위해 추가
import com.OnTime.ontime.NotificationBuilder
import com.OnTime.ontime.data.models.NotificationTone
import com.OnTime.ontime.data.models.UserPreferences
import com.OnTime.ontime.BuildConfig // BuildConfig 임포트

/**
 * 알림 톤 설정을 위한 UI 컴포넌트입니다.
 * 기존 설정 화면에 이 Composable을 추가하세요.
 *
 * @param currentTone 현재 설정된 알림 톤
 * @param onToneSelected 새로운 톤이 선택되었을 때 호출될 콜백 함수
 */
@Composable
fun NotificationSettingsSection(
    currentTone: NotificationTone,
    onToneSelected: (NotificationTone) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("알림 메시지 톤", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp)) // Corrected here
        Card {
            Column(Modifier.padding(vertical = 8.dp)) {
                NotificationTone.values().forEach { tone ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (tone == currentTone),
                            onClick = { onToneSelected(tone) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tone.description)
                    }
                }
            }
        }
    }
}

/**
 * 알림 횟수 설정을 위한 UI 컴포넌트입니다.
 *
 * @param currentCount 현재 설정된 알림 횟수
 * @param onCountChanged 새로운 횟수가 선택되었을 때 호출될 콜백 함수
 */
@Composable
fun NotificationCountSetting(
    currentCount: Int,
    onCountChanged: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("알림 횟수", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp)) // Corrected here
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "일정 당 받을 알림 횟수를 설정합니다: $currentCount 회")
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = currentCount.toFloat(),
                    onValueChange = { onCountChanged(it.roundToInt()) },
                    valueRange = 1f..5f, // 1회부터 5회까지
                    steps = 3 // 1, 2, 3, 4, 5
                )
            }
        }
    }
}

/**
 * 설정 테스트를 위한 UI 컴포넌트입니다.
 * 설정 화면의 적절한 위치에 추가하여 테스트할 수 있습니다.
 *
 * @param testTone 테스트에 사용할 알림 톤
 */
@Composable
fun NotificationTestSection(
    testTone: NotificationTone
) {
    var generatedMessage by remember { mutableStateOf("버튼을 눌러 테스트 알림을 생성하세요.") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val notificationBuilder = remember { NotificationBuilder() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(24.dp)) // Corrected here
        Text("설정 테스트", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp)) // Corrected here

        // --- 테스트 실행 버튼 ---
        Button(
            onClick = {
                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    generatedMessage = "오류: GEMINI_API_KEY가 설정되지 않았습니다."
                    return@Button
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    generatedMessage = "오류: 알림 테스트는 API 레벨 31 이상에서만 지원됩니다."
                    isLoading = false
                    return@Button
                }

                isLoading = true
                generatedMessage = "Gemini가 메시지를 생성 중입니다..."
                coroutineScope.launch {
                    val preferences = UserPreferences(notificationTone = testTone)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val message = notificationBuilder.generateNotificationMessage(
                            userPreferences = preferences,
                            eventName = "테스트 약속",
                            eventTime = LocalTime.of(10, 0), // API Level 26 이상에서만 실행됨
                            travelTime = 30,
                            actualTravelTime = null, // Explicitly pass null for actualTravelTime
                            weatherInfo = "맑음",
                            userPattern = null
                        )
                        generatedMessage = message ?: "메시지 생성에 실패했습니다. API 키나 네트워크를 확인해주세요."
                    } else {
                        generatedMessage = "오류: 알림 테스트는 API 레벨 31 이상에서만 지원됩니다."
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("테스트 알림 생성 (${testTone.description})")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 결과 표시 ---
        Card(modifier = Modifier.fillMaxWidth()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
            } else {
                Text(
                    text = generatedMessage,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}