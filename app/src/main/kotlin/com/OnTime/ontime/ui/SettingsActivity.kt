package com.OnTime.ontime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.OnTime.ontime.data.models.NotificationTone
import com.OnTime.ontime.data.models.TransportMode
import com.OnTime.ontime.data.models.UserPreferences
import com.OnTime.ontime.data.repositories.SettingsRepository
import com.OnTime.ontime.ui.theme.OnTimeTheme
import kotlinx.coroutines.launch // Import for coroutineScope.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnTimeTheme {
                // SettingsScreen is now responsible for creating its own SettingsRepository
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var transportMode by remember { mutableStateOf(UserPreferences().transportMode) }
    var notificationTone by remember { mutableStateOf(UserPreferences().notificationTone) }
    var notificationCount by remember { mutableStateOf(UserPreferences().notificationCount) }

    // Load initial preferences when the Composable first enters the composition
    LaunchedEffect(Unit) {
        val initialPreferences = settingsRepository.getUserPreferences()
        transportMode = initialPreferences.transportMode
        notificationTone = initialPreferences.notificationTone
        notificationCount = initialPreferences.notificationCount
    }

    // Function to save settings to repository
    val onSettingsChanged: () -> Unit = {
        val newPreferences = UserPreferences(transportMode, notificationTone, notificationCount)
        coroutineScope.launch {
            settingsRepository.saveUserPreferences(newPreferences)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp)) // Fix for vertical arrangement spacing

            // --- 1. 기본 이동 수단 설정 (기존 UI 재사용) ---
            TransportModeSetting(
                selectedMode = transportMode,
                onModeSelected = {
                    transportMode = it
                    onSettingsChanged()
                }
            )

            // --- 2. 알림 메시지 톤 설정 (새로운 UI로 교체) ---
            NotificationSettingsSection(
                currentTone = notificationTone,
                onToneSelected = {
                    notificationTone = it
                    onSettingsChanged()
                }
            )

            // --- 3. 알림 횟수 설정 (새로운 UI 추가) ---
            NotificationCountSetting(
                currentCount = notificationCount,
                onCountChanged = { newCount ->
                    notificationCount = newCount
                    onSettingsChanged()
                }
            )

            // --- 4. 설정 테스트 UI 추가 ---
            Divider(modifier = Modifier.padding(top = 8.dp))
            NotificationTestSection(testTone = notificationTone)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportModeSetting(
    selectedMode: TransportMode,
    onModeSelected: (TransportMode) -> Unit
) {
    val modes = com.OnTime.ontime.data.models.TransportMode.values()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "기본 이동 수단", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    onClick = { onModeSelected(mode) },
                    selected = mode == selectedMode,
                    icon = { 
                        Icon(
                            imageVector = mode.toIcon(), 
                            contentDescription = mode.toDisplayString()
                        )
                    }
                ) {
                    Text(mode.toDisplayString())
                }
            }
        }
    }
}

// Helper functions (기존 코드와 동일)
fun TransportMode.toDisplayString(): String {
    return when (this) {
        TransportMode.WALKING -> "도보"
        TransportMode.DRIVING -> "자가용"
        TransportMode.TRANSIT -> "대중교통"
    }
}

fun TransportMode.toIcon(): ImageVector {
    return when (this) {
        TransportMode.WALKING -> Icons.Default.DirectionsWalk
        TransportMode.DRIVING -> Icons.Default.DirectionsCar
        TransportMode.TRANSIT -> Icons.Default.DirectionsTransit
    }
}