package com.OnTime.ontime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.OnTime.ontime.data.models.NotificationHistory
import com.OnTime.ontime.service.NotificationHistoryService
import com.OnTime.ontime.ui.theme.OnTimeTheme
import java.text.SimpleDateFormat
import java.util.*

class NotificationHistoryActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val historyService = NotificationHistoryService(this)
        
        setContent {
            OnTimeTheme {
                NotificationHistoryScreen(
                    historyService = historyService,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    historyService: NotificationHistoryService,
    onBackClick: () -> Unit
) {
    var notifications by remember { mutableStateOf(historyService.getNotificationHistory()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("알림 히스토리") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        historyService.clearHistory()
                        notifications = emptyList()
                    }) {
                        Icon(Icons.Default.Delete, "전체 삭제")
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("알림 히스토리가 없습니다")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    NotificationHistoryCard(notification)
                }
            }
        }
    }
}

@Composable
fun NotificationHistoryCard(notification: NotificationHistory) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when (notification.type) {
                        "RAG" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        "TRAVEL" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        "ERROR" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                )
                .padding(16.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(notification.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 메시지
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // RAG 상세 정보
            if (notification.type == "RAG" && notification.eventTitle != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "📅 일정: ${notification.eventTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        notification.myLocation?.let {
                            Text(
                                text = "📍 내 위치: $it",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        notification.extractedLocation?.let {
                            Text(
                                text = "🎯 목적지: $it",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        notification.travelTime?.let {
                            Text(
                                text = "🚇 이동시간: $it",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        notification.ragMessage?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🤖 RAG 메시지: $it",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
