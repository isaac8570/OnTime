package com.OnTime.ontime.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.OnTime.ontime.NotificationBuilder // Import NotificationBuilder
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.SettingsRepository // Import SettingsRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.service.AndroidLocationService
import com.OnTime.ontime.ui.screen.login.LoginActivity
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.ui.viewmodel.CalendarViewModel
import com.OnTime.ontime.ui.viewmodel.MainViewModel
import com.OnTime.ontime.ui.viewmodel.ViewModelFactory
import com.OnTime.ontime.util.Constants
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
        val notificationBuilder = NotificationBuilder() // Instantiate NotificationBuilder
        val settingsRepository = SettingsRepository(app) // Instantiate SettingsRepository

        ViewModelFactory(
            application = app,
            calendarRepository = calendarRepository,
            weatherRepository = weatherRepository,
            locationRepository = locationRepository,
            androidLocationService = androidLocationService,
            notificationBuilder = notificationBuilder, // Pass to factory
            settingsRepository = settingsRepository // Pass to factory
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    calendarViewModel: CalendarViewModel,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_CALENDAR
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val areGranted = permissionsMap.values.all { it }
        if (areGranted) {
            calendarViewModel.loadEventsWithTravelTime()
        } else {
            Toast.makeText(context, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val arePermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!arePermissionsGranted) {
            launcher.launch(permissions)
        } else {
            calendarViewModel.loadEventsWithTravelTime()
        }
    }

    val events by calendarViewModel.events.observeAsState(initial = emptyList())
    val isLoadingEvents by calendarViewModel.isLoading.observeAsState(initial = false)
    val isLoadingMore by calendarViewModel.isLoadingMore.observeAsState(initial = false)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("OnTime", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, AlarmTestActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Alarm, "테스트 알림", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, CustomLocationSettingsActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.Person, "사용자 정의 위치 설정", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, NotificationListPageActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Outlined.Notifications, "알림 내역", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "설정", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInClient.signOut().addOnCompleteListener {
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, "로그아웃", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoadingEvents && events.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 5.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "캘린더의 일정을 불러오고 있어요!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (events.isEmpty()) {
                EmptyState(Modifier.align(Alignment.Center))
            } else {
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        ModernEventCard(
                            event = event,
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📅",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "오늘의 일정이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Google 캘린더에 일정을 추가해보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModernEventCard(event: CalendarEvent, modifier: Modifier = Modifier) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
    val now = System.currentTimeMillis()
    val timeUntil = (event.startTime - now).coerceAtLeast(0) / (60 * 1000) // Ensure non-negative

    val urgencyColor = when {
        timeUntil < 30 -> MaterialTheme.colorScheme.secondary
        timeUntil < 60 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.primary
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(durationMillis = 500))
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, urgencyColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = event.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            timeUntil < 1 -> "곧 시작"
                            timeUntil < 60 -> "${timeUntil}분 후"
                            timeUntil < 1440 -> "${timeUntil / 60}시간 후"
                            else -> "${timeUntil / 1440}일 후"
                        },
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .background(urgencyColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Rows
                InfoRow(icon = Icons.Outlined.AccessTime, text = "${SimpleDateFormat("MM월 dd일 HH:mm", Locale.KOREA).format(Date(event.startTime))} 시작")

                event.location?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(icon = Icons.Outlined.LocationOn, text = it)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Info Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val travelIcon = when (event.finalTravelMode) {
                        Constants.MODE_DRIVING -> Icons.Outlined.DirectionsCar
                        Constants.MODE_WALKING -> Icons.Outlined.DirectionsWalk
                        else -> Icons.Outlined.DirectionsTransit
                    }
                    InfoChip(
                        modifier = Modifier.weight(1f),
                        icon = travelIcon,
                        label = "예상 이동시간",
                        value = event.travelDuration ?: "..."
                    )
                    InfoChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Notifications,
                        label = "추천 출발시간",
                        value = event.departureTime?.let { timeFormat.format(Date(it)) } ?: "..."
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoChip(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}