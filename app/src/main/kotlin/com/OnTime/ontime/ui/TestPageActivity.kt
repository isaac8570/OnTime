package com.OnTime.ontime.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState // Import rememberScrollState
import androidx.compose.foundation.verticalScroll // Import verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.OnTime.ontime.NotificationBuilder
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.models.TravelLog
import com.OnTime.ontime.data.models.UserPreferences
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.SettingsRepository
import com.OnTime.ontime.data.repositories.TravelDataRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.util.LocationConverter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class TestPageActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var travelDataRepository: TravelDataRepository
    private lateinit var notificationBuilder: NotificationBuilder // Declare NotificationBuilder
    private lateinit var settingsRepository: SettingsRepository // Declare SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        calendarRepository = CalendarRepository(application)
        locationRepository = LocationRepository(application)
        weatherRepository = WeatherRepository()
        travelDataRepository = TravelDataRepository()
        notificationBuilder = NotificationBuilder() // Initialize NotificationBuilder
        settingsRepository = SettingsRepository(application) // Initialize SettingsRepository

        setContent {
            OnTimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TestPageScreen(
                        fusedLocationClient = fusedLocationClient,
                        calendarRepository = calendarRepository,
                        locationRepository = locationRepository,
                        weatherRepository = weatherRepository,
                        travelDataRepository = travelDataRepository,
                        notificationBuilder = notificationBuilder, // Pass NotificationBuilder
                        settingsRepository = settingsRepository // Pass SettingsRepository
                    )
                }
            }
        }
    }
}

@Composable
fun TestPageScreen(
    fusedLocationClient: FusedLocationProviderClient,
    calendarRepository: CalendarRepository,
    locationRepository: LocationRepository,
    weatherRepository: WeatherRepository,
    travelDataRepository: TravelDataRepository,
    notificationBuilder: NotificationBuilder, // Receive NotificationBuilder
    settingsRepository: SettingsRepository // Receive SettingsRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var currentAddress by remember { mutableStateOf<String?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    var calendarEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var calendarPermissionGranted by remember { mutableStateOf(false) }

    var estimatedTravelTime by remember { mutableStateOf<String>("정보 없음") }
    var estimatedTravelTimeMinutes by remember { mutableStateOf<Int?>(null) }
    var currentWeather by remember { mutableStateOf<String>("정보 없음") }

    var originLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var destinationLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // State for automatic actual travel time calculation
    var isTrackingTrip by remember { mutableStateOf(false) }
    var tripStartTime by remember { mutableStateOf<Long?>(null) }
    var initialTrackingLocation by remember { mutableStateOf<Location?>(null) }
    var actualCalculatedTravelTimeMinutes by remember { mutableStateOf<Int?>(null) }
    var distanceToDestination by remember { mutableStateOf<Float?>(null) }
    var distanceMovedFromOrigin by remember { mutableStateOf<Float?>(null) }

    // States for personalized notification testing
    var generatedTestMessage by remember { mutableStateOf("버튼을 눌러 맞춤 알림을 생성하세요.") }
    var isGeneratingTestMessage by remember { mutableStateOf(false) }


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        calendarPermissionGranted = permissions[Manifest.permission.READ_CALENDAR] == true

        if (locationPermissionGranted) {
            getCurrentLocation(context, fusedLocationClient) { location ->
                currentLatitude = location?.latitude
                currentLongitude = location?.longitude
                if (location != null) {
                    originLatLng = Pair(location.latitude, location.longitude)
                    coroutineScope.launch {
                        currentAddress = LocationConverter.latLngToAddress(context, location.latitude, location.longitude)
                    }
                }
            }
        }
        if (calendarPermissionGranted) {
            coroutineScope.launch {
                val (events, _) = calendarRepository.getEvents(15)
                calendarEvents = events
            }
        }
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        locationPermissionGranted = fineLocationGranted || coarseLocationGranted

        val readCalendarGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        calendarPermissionGranted = readCalendarGranted


        val permissionsToRequest = mutableListOf<String>()
        if (!locationPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!calendarPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.READ_CALENDAR)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            if (locationPermissionGranted) {
                getCurrentLocation(context, fusedLocationClient) { location ->
                    currentLatitude = location?.latitude
                    currentLongitude = location?.longitude
                    if (location != null) {
                        originLatLng = Pair(location.latitude, location.longitude)
                        coroutineScope.launch {
                            currentAddress = LocationConverter.latLngToAddress(context, location.latitude, location.longitude)
                        }
                    }
                }
            }
            if (calendarPermissionGranted) {
                coroutineScope.launch {
                    val (events, _) = calendarRepository.getEvents(15)
                    calendarEvents = events
                }
            }
        }
    }

    // Effect to calculate estimated travel time
    LaunchedEffect(originLatLng, selectedEvent) {
        val destinationAddress = selectedEvent?.location

        if (originLatLng != null && destinationAddress != null && destinationAddress.isNotBlank()) {
            coroutineScope.launch {
                val resolvedDestinationLatLng = LocationConverter.addressToLatLng(context, destinationAddress)
                destinationLatLng = resolvedDestinationLatLng
                if (resolvedDestinationLatLng != null) {
                    val travelInfo = locationRepository.calculateTravelTime(originLatLng!!, resolvedDestinationLatLng)
                    estimatedTravelTime = travelInfo?.durationText ?: "정보 없음"
                    estimatedTravelTimeMinutes = travelInfo?.durationMinutes
                } else {
                    estimatedTravelTime = "정보 없음 (목적지 주소 확인 불가)"
                    estimatedTravelTimeMinutes = null
                }
            }
        } else {
            estimatedTravelTime = "정보 없음 (출발지 또는 목적지 누락)"
            estimatedTravelTimeMinutes = null
        }
    }

    // Effect to fetch weather
    LaunchedEffect(currentLatitude, currentLongitude) {
        if (currentLatitude != null && currentLongitude != null) {
            coroutineScope.launch {
                val gridCoords = LocationConverter.latLngToKmaGrid(currentLatitude!!, currentLongitude!!)
                currentWeather = weatherRepository.getWeatherCondition(gridCoords)
            }
        } else {
            currentWeather = "정보 없음 (위치 정보 누락)"
        }
    }

    // Effect for real-time location tracking, departure detection, and arrival detection
    LaunchedEffect(isTrackingTrip, currentLatitude, currentLongitude, initialTrackingLocation, destinationLatLng) {
        if (isTrackingTrip && currentLatitude != null && currentLongitude != null && destinationLatLng != null) {
            val currentLoc = Location("current").apply {
                latitude = currentLatitude!!
                longitude = currentLongitude!!
            }
            val destLoc = Location("destination").apply {
                latitude = destinationLatLng!!.first
                longitude = destinationLatLng!!.second
            }
            distanceToDestination = currentLoc.distanceTo(destLoc)

            // Departure Detection
            if (tripStartTime == null && initialTrackingLocation != null) {
                distanceMovedFromOrigin = currentLoc.distanceTo(initialTrackingLocation!!)
                if (distanceMovedFromOrigin != null && distanceMovedFromOrigin!! > 50) {
                    tripStartTime = System.currentTimeMillis()
                    Toast.makeText(context, "출발 감지! 실제 이동 시간 측정 시작.", Toast.LENGTH_SHORT).show()
                }
            }

            // Arrival Detection (only if tripStartTime is set)
            if (tripStartTime != null && distanceToDestination != null && distanceToDestination!! < 50) {
                val elapsedTimeMillis = System.currentTimeMillis() - tripStartTime!!
                actualCalculatedTravelTimeMinutes = (elapsedTimeMillis.toDouble() / (1000 * 60)).roundToInt()
                Toast.makeText(context, "목적지에 도착했습니다! 실제 이동 시간: ${actualCalculatedTravelTimeMinutes}분", Toast.LENGTH_LONG).show()
                isTrackingTrip = false
                tripStartTime = null
                initialTrackingLocation = null
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Added for scrolling
    ) {
        Text("데이터 수집 및 로직 검증 테스트 페이지", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Current Location Section
        Text("현재 위치:", style = MaterialTheme.typography.titleMedium)
        if (locationPermissionGranted) {
            Text("위도: ${currentLatitude ?: "정보 없음"}")
            Text("경도: ${currentLongitude ?: "정보 없음"}")
            Text("주소: ${currentAddress ?: "주소 확인 중..."}")
        } else {
            Text("위치 권한이 허용되지 않았습니다.")
            Button(onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_CALENDAR
                    )
                )
            }) {
                Text("권한 요청")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Events Section
        Text("캘린더 이벤트:", style = MaterialTheme.typography.titleMedium)
        if (calendarPermissionGranted) {
            if (calendarEvents.isEmpty()) {
                Text("다가오는 일정이 없거나 로딩 중입니다...")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(calendarEvents) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedEvent = event }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("제목: ${event.title}")
                                Text("시간: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(event.startTime))}")
                                Text("장소: ${event.location ?: "정보 없음"}")
                            }
                        }
                    }
                }
            }
            selectedEvent?.let { event ->
                Spacer(modifier = Modifier.height(8.dp))
                Text("선택된 이벤트 상세 정보:", style = MaterialTheme.typography.titleSmall)
                Text("제목: ${event.title}")
                Text("장소: ${event.location ?: "정보 없음"}")
                Text("시작 시간: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(event.startTime))}")
            }
        } else {
            Text("캘린더 권한이 허용되지 않았습니다.")
            Button(onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_CALENDAR
                    )
                )
            }) {
                Text("권한 요청")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Estimated Travel Time Section
        Text("예상 이동 시간 (Google 지도):", style = MaterialTheme.typography.titleMedium)
        Text("시간: $estimatedTravelTime")
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Current Weather Section
        Text("현재 날씨:", style = MaterialTheme.typography.titleMedium)
        Text("상태: $currentWeather")
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Actual Travel Time Tracking Section
        Text("실제 이동 시간 (자동 계산):", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = {
                    if (originLatLng == null || destinationLatLng == null || selectedEvent == null || currentLatitude == null || currentLongitude == null) {
                        Toast.makeText(context, "현재 위치와 목적지(이벤트)를 선택해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isTrackingTrip = true
                    tripStartTime = null
                    initialTrackingLocation = Location("initial").apply {
                        latitude = currentLatitude!!
                        longitude = currentLongitude!!
                    }
                    actualCalculatedTravelTimeMinutes = null
                    Toast.makeText(context, "여행 추적 시작! 출발 감지 대기 중...", Toast.LENGTH_SHORT).show()
                },
                enabled = !isTrackingTrip && originLatLng != null && destinationLatLng != null && selectedEvent != null
            ) {
                Text("여행 추적 시작")
            }
            Button(
                onClick = {
                    if (isTrackingTrip) {
                        isTrackingTrip = false
                        tripStartTime = null
                        initialTrackingLocation = null
                        Toast.makeText(context, "추적 중지됨.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "추적 중이 아닙니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = isTrackingTrip
            ) {
                Text("추적 중지")
            }
        }
        if (isTrackingTrip) {
            if (tripStartTime == null) {
                Text("출발 감지 대기 중... (초기 위치에서 ${distanceMovedFromOrigin?.roundToInt() ?: "정보 없음"}m 이동)")
            } else {
                val elapsedTime = (System.currentTimeMillis() - tripStartTime!!) / (1000 * 60)
                Text("추적 중... 경과 시간: ${elapsedTime}분, 목적지까지 ${distanceToDestination?.roundToInt() ?: "정보 없음"}m 남음")
            }
        } else {
            Text("자동 계산된 시간: ${actualCalculatedTravelTimeMinutes ?: "정보 없음"}분")
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Personalized Notification Test Section
        Text("개인 맞춤 알림 테스트:", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = {
                    if (selectedEvent == null || estimatedTravelTimeMinutes == null || currentWeather == "정보 없음") {
                        Toast.makeText(context, "이벤트를 선택하고 예상 이동 시간, 날씨를 가져와야 합니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGeneratingTestMessage = true
                    coroutineScope.launch {
                        val userPreferences = settingsRepository.getUserPreferences()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val message = notificationBuilder.generateNotificationMessage(
                                userPreferences = userPreferences,
                                eventName = selectedEvent!!.title,
                                eventTime = LocalTime.ofInstant(Instant.ofEpochMilli(selectedEvent!!.startTime), ZoneId.systemDefault()),
                                travelTime = estimatedTravelTimeMinutes!!,
                                actualTravelTime = estimatedTravelTimeMinutes!! + 5, // 5분 지각 시뮬레이션
                                weatherInfo = currentWeather,
                                userPattern = null
                            )
                            generatedTestMessage = message ?: "메시지 생성 실패"
                        } else {
                            generatedTestMessage = "API level S 이상이 필요합니다."
                        }
                        isGeneratingTestMessage = false
                    }
                },
                enabled = !isGeneratingTestMessage && selectedEvent != null && estimatedTravelTimeMinutes != null && currentWeather != "정보 없음"
            ) {
                Text("5분 지각 시뮬레이션")
            }
            Button(
                onClick = {
                    if (selectedEvent == null || estimatedTravelTimeMinutes == null || currentWeather == "정보 없음") {
                        Toast.makeText(context, "이벤트를 선택하고 예상 이동 시간, 날씨를 가져와야 합니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGeneratingTestMessage = true
                    coroutineScope.launch {
                        val userPreferences = settingsRepository.getUserPreferences()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val message = notificationBuilder.generateNotificationMessage(
                                userPreferences = userPreferences,
                                eventName = selectedEvent!!.title,
                                eventTime = LocalTime.ofInstant(Instant.ofEpochMilli(selectedEvent!!.startTime), ZoneId.systemDefault()),
                                travelTime = estimatedTravelTimeMinutes!!,
                                actualTravelTime = estimatedTravelTimeMinutes!! - 5, // 5분 일찍 도착 시뮬레이션
                                weatherInfo = currentWeather,
                                userPattern = null
                            )
                            generatedTestMessage = message ?: "메시지 생성 실패"
                        } else {
                            generatedTestMessage = "API level S 이상이 필요합니다."
                        }
                        isGeneratingTestMessage = false
                    }
                },
                enabled = !isGeneratingTestMessage && selectedEvent != null && estimatedTravelTimeMinutes != null && currentWeather != "정보 없음"
            ) {
                Text("5분 일찍 도착 시뮬레이션")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = {
                    if (selectedEvent == null || estimatedTravelTimeMinutes == null || currentWeather == "정보 없음") {
                        Toast.makeText(context, "이벤트를 선택하고 예상 이동 시간, 날씨를 가져와야 합니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGeneratingTestMessage = true
                    coroutineScope.launch {
                        val userPreferences = settingsRepository.getUserPreferences()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val message = notificationBuilder.generateNotificationMessage(
                                userPreferences = userPreferences,
                                eventName = selectedEvent!!.title,
                                eventTime = LocalTime.ofInstant(Instant.ofEpochMilli(selectedEvent!!.startTime), ZoneId.systemDefault()),
                                travelTime = estimatedTravelTimeMinutes!!,
                                actualTravelTime = estimatedTravelTimeMinutes!!, // 정시 도착 시뮬레이션
                                weatherInfo = currentWeather,
                                userPattern = null
                            )
                            generatedTestMessage = message ?: "메시지 생성 실패"
                        } else {
                            generatedTestMessage = "API level S 이상이 필요합니다."
                        }
                        isGeneratingTestMessage = false
                    }
                },
                enabled = !isGeneratingTestMessage && selectedEvent != null && estimatedTravelTimeMinutes != null && currentWeather != "정보 없음"
            ) {
                Text("정시 도착 시뮬레이션")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (isGeneratingTestMessage) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Text(text = generatedTestMessage, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))


        // Save to Firebase Button
        Button(
            onClick = {
                if (selectedEvent == null || originLatLng == null || destinationLatLng == null || actualCalculatedTravelTimeMinutes == null || estimatedTravelTimeMinutes == null) {
                    Toast.makeText(context, "모든 정보를 입력하고 이벤트를 선택한 후 실제 이동 시간을 계산해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    coroutineScope.launch {
                        val eventDate = Date(selectedEvent!!.startTime)
                        val travelLog = TravelLog(
                            eventId = selectedEvent!!.id,
                            eventTitle = selectedEvent!!.title,
                            originLat = originLatLng!!.first,
                            originLng = originLatLng!!.second,
                            destinationLat = destinationLatLng!!.first,
                            destinationLng = destinationLatLng!!.second,
                            googleEtaMin = estimatedTravelTimeMinutes!!,
                            actualEtaMin = actualCalculatedTravelTimeMinutes!!,
                            weather = currentWeather,
                            hourOfDay = SimpleDateFormat("HH", Locale.getDefault()).format(eventDate).toInt(),
                            dayOfWeek = SimpleDateFormat("u", Locale.getDefault()).format(eventDate).toInt(), // 1 for Monday, 7 for Sunday
                            distanceKm = 0.0, // Placeholder for now. Needs calculation.
                            timestamp = Date()
                        )
                        try {
                            travelDataRepository.saveTravelLog(travelLog)
                            Toast.makeText(context, "이동 기록 저장 완료!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "이동 기록 저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = actualCalculatedTravelTimeMinutes != null // Enable only after calculation
        ) {
            Text("Firebase에 이동 기록 저장")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun getCurrentLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationResult: (Location?) -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                onLocationResult(location)
            }
            .addOnFailureListener { e ->
                println("Error getting location: ${e.message}")
                onLocationResult(null)
            }
    } else {
        onLocationResult(null) // Permissions not granted
    }
}