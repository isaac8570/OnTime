package com.OnTime.ontime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.OnTime.ontime.data.models.NotificationRecord
import com.OnTime.ontime.data.repositories.NotificationRepository
import com.OnTime.ontime.ui.theme.OnTimeTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationListPageActivity : ComponentActivity() {
    private lateinit var notificationRepository: NotificationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationRepository = NotificationRepository()

        setContent {
            OnTimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotificationListScreen(notificationRepository = notificationRepository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(notificationRepository: NotificationRepository) {
    val coroutineScope = rememberCoroutineScope()
    var notificationRecords by remember { mutableStateOf<List<NotificationRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val refreshNotifications: suspend () -> Unit = {
        isLoading = true
        try {
            notificationRecords = notificationRepository.getNotificationRecords()
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "알림 기록을 불러오는 데 실패했습니다: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("알림 내역") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            } else if (notificationRecords.isEmpty()) {
                Text("저장된 알림 내역이 없습니다.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notificationRecords) { record ->
                        NotificationRecordCard(record = record)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationRecordCard(record: NotificationRecord) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("제목: ${record.eventTitle}", style = MaterialTheme.typography.titleMedium)
            Text("메시지: ${record.notificationMessage}", style = MaterialTheme.typography.bodyMedium)
            Text("생성 시간: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(record.timestamp)}", style = MaterialTheme.typography.bodySmall)
            Text("예상 이동 시간: ${record.estimatedTravelTimeMinutes}분", style = MaterialTheme.typography.bodySmall)
            Text("실제 이동 시간: ${record.actualTravelTimeMinutes}분", style = MaterialTheme.typography.bodySmall)
            Text("날씨: ${record.weatherInfo}", style = MaterialTheme.typography.bodySmall)
            Text("메시지 톤: ${record.messageTone}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
