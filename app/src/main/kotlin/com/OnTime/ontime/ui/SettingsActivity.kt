package com.OnTime.ontime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.OnTime.ontime.data.models.NotificationTone
import com.OnTime.ontime.data.models.TransportMode
import com.OnTime.ontime.data.models.UserPreferences
import com.OnTime.ontime.data.repositories.SettingsRepository
import com.OnTime.ontime.ui.theme.OnTimeTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)

        setContent {
            OnTimeTheme {
                SettingsScreen(settingsRepository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsRepository: SettingsRepository) {
    val initialPreferences = remember { settingsRepository.getUserPreferences() }
    var transportMode by remember { mutableStateOf(initialPreferences.transportMode) }
    var notificationTone by remember { mutableStateOf(initialPreferences.notificationTone) }

    val onSettingsChanged = {
        val newPreferences = UserPreferences(transportMode, notificationTone)
        settingsRepository.saveUserPreferences(newPreferences)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TransportModeSetting(
                selectedMode = transportMode,
                onModeSelected = {
                    transportMode = it
                    onSettingsChanged()
                }
            )
            NotificationToneSetting(
                selectedTone = notificationTone,
                onToneSelected = {
                    notificationTone = it
                    onSettingsChanged()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportModeSetting(
    selectedMode: TransportMode,
    onModeSelected: (TransportMode) -> Unit
) {
    val modes = TransportMode.values()
    
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "기본 이동 수단", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "경로 계산 시 사용할 기본 이동 수단을 선택하세요.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
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
}

@Composable
fun NotificationToneSetting(
    selectedTone: NotificationTone,
    onToneSelected: (NotificationTone) -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        ListItem(
            headlineContent = { Text("알림 메시지 톤", style = MaterialTheme.typography.titleLarge) },
            supportingContent = { Text("AI 비서가 보낼 알림의 어조를 설정합니다.") },
            trailingContent = {
                Switch(
                    checked = selectedTone == NotificationTone.STRONG,
                    onCheckedChange = { isChecked ->
                        onToneSelected(if (isChecked) NotificationTone.STRONG else NotificationTone.SOFT)
                    }
                )
            }
        )
    }
}

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
