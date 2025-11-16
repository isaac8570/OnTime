package com.OnTime.ontime.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.OnTime.ontime.DataCollectionActivity
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.service.AndroidLocationService
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.ui.viewmodel.CalendarViewModel
import com.OnTime.ontime.ui.viewmodel.MainViewModel
import com.OnTime.ontime.ui.viewmodel.ViewModelFactory
// ✅ 이 줄이 추가되었습니다!
import com.OnTime.ontime.util.LocationConverter
import com.OnTime.ontime.worker.CalendarSyncWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val factory: ViewModelFactory by lazy {
        val app = application
        val calendarRepository = CalendarRepository(app)
        val weatherRepository = WeatherRepository()
        val locationRepository = LocationRepository(app)
        val androidLocationService = AndroidLocationService(app)

        ViewModelFactory(
            application = app,
            calendarRepository = calendarRepository,
            weatherRepository = weatherRepository,
            locationRepository = locationRepository,
            androidLocationService = androidLocationService
        )
    }

    private val calendarViewModel: CalendarViewModel by viewModels { factory }
    private val mainViewModel: MainViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule CalendarSyncWorker
        val calendarSyncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
            15, TimeUnit.MINUTES // Run every 15 minutes
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "CalendarSyncWork",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already enqueued
            calendarSyncRequest
        )

        setContent {
            OnTimeTheme {
                MainScreen(
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    calendarViewModel = calendarViewModel,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}

// 나머지 코드는 이전과 동일합니다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    calendarViewModel: CalendarViewModel,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val areGranted = permissionsMap.values.all { it }
    }

    LaunchedEffect(Unit) {
        val arePermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!arePermissionsGranted) {
            launcher.launch(permissions)
        }
        calendarViewModel.loadEventsWithTravelTime()
    }

    val events by calendarViewModel.events.observeAsState(initial = emptyList())
    val isLoadingEvents by calendarViewModel.isLoading.observeAsState(initial = false)
    val notificationMessage by mainViewModel.notificationMessage.observeAsState()
    val weatherStatus by mainViewModel.weatherStatus.observeAsState()
    val isLoadingMessage by mainViewModel.isLoading.observeAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OnTime", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("오늘의 일정", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    /* Removed NotificationHistoryActivity navigation
                    IconButton(onClick = {
                        val intent = Intent(context, NotificationHistoryActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Outlined.Notifications, "알림 히스토리")
                    }
                    */
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "설정")
                    }
                    IconButton(onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInClient.signOut().addOnCompleteListener {
                            val intent = Intent(context, AuthActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, "로그아웃")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    calendarViewModel.testRAGSystem()
                }
            ) {
                Text("🧠")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            // Section for Weather and ETA logic
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card for Weather Check
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoadingMessage && weatherStatus == null) {
                            CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                        } else {
                            Text(
                                text = weatherStatus ?: "버튼을 눌러 현재 날씨를 확인하세요.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Button(onClick = { mainViewModel.checkCurrentWeather() }) {
                            Text("날씨 확인하기")
                        }
                    }
                }

                // Card for ETA Notification
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoadingMessage && notificationMessage == null) {
                            CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                        } else {
                            Text(
                                text = notificationMessage ?: "버튼을 눌러 예상 시간을 확인하세요.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Button(onClick = { mainViewModel.generateRealtimeNotificationMessage() }) {
                            Text("예상 시간 확인하기 (실시간)")
                        }
                    }
                }

                // Button to launch DataCollectionActivity
                Button(
                    onClick = {
                        val intent = Intent(context, DataCollectionActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("데이터 수집 페이지로 이동")
                }

                // Button to launch TestPageActivity
                Button(
                    onClick = {
                        val intent = Intent(context, TestPageActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("테스트 페이지로 이동")
                }
            }


            // Existing UI for calendar events
            Box(modifier = Modifier.weight(1f)) {
                if (isLoadingEvents) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (events.isEmpty()) {
                    EmptyState(Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(events) { event ->
                            ModernEventCard(event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📅",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "일정이 없습니다",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Google 캘린더에 일정을 추가해보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModernEventCard(event: CalendarEvent) {
    val dateFormat = SimpleDateFormat("MM월 dd일", Locale.KOREA)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
    val now = System.currentTimeMillis()
    val timeUntil = (event.startTime - now) / (60 * 1000)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    event.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        timeUntil < 30 -> MaterialTheme.colorScheme.errorContainer
                        timeUntil < 60 -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = when {
                            timeUntil < 60 -> "${timeUntil}분 후"
                            timeUntil < 1440 -> "${timeUntil / 60}시간 후"
                            else -> "${timeUntil / 1440}일 후"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${dateFormat.format(Date(event.startTime))} ${timeFormat.format(Date(event.startTime))}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            event.location?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "도착: $it",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Outlined.DirectionsCar,
                    label = "이동시간",
                    value = event.travelDuration ?: "계산 중..."
                )
                InfoChip(
                    icon = Icons.Outlined.Notifications,
                    label = "출발시간",
                    value = "계산 중..."
                )
            }
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
