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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.models.TravelLog
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.TravelDataRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.util.LocationConverter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestPageActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var travelDataRepository: TravelDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        calendarRepository = CalendarRepository(application)
        locationRepository = LocationRepository(application)
        weatherRepository = WeatherRepository()
        travelDataRepository = TravelDataRepository()

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
                        travelDataRepository = travelDataRepository
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
    travelDataRepository: TravelDataRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    var calendarEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var calendarPermissionGranted by remember { mutableStateOf(false) }

    var estimatedTravelTime by remember { mutableStateOf<String>("정보 없음") }
    var estimatedTravelTimeMinutes by remember { mutableStateOf<Int?>(null) }
    var currentWeather by remember { mutableStateOf<String>("정보 없음") }

    var actualTravelTimeInput by remember { mutableStateOf("") }
    val actualTravelTimeMinutes = actualTravelTimeInput.toIntOrNull()

    var originLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var destinationLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }


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
                }
            }
        }
        if (calendarPermissionGranted) {
            coroutineScope.launch {
                calendarEvents = calendarRepository.getEvents()
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
                    }
                }
            }
            if (calendarPermissionGranted) {
                calendarEvents = calendarRepository.getEvents()
            }
        }
    }

    // Effect to calculate estimated travel time
    LaunchedEffect(originLatLng, selectedEvent) {
        val destinationAddress = selectedEvent?.location

        if (originLatLng != null && destinationAddress != null && destinationAddress.isNotBlank()) {
            coroutineScope.launch {
                val resolvedDestinationLatLng = LocationConverter.addressToLatLng(context, destinationAddress)
                destinationLatLng = resolvedDestinationLatLng // Store resolved destination LatLng
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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("데이터 수집 및 로직 검증 테스트 페이지", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Current Location Section
        Text("현재 위치:", style = MaterialTheme.typography.titleMedium)
        if (locationPermissionGranted) {
            Text("위도: ${currentLatitude ?: "정보 없음"}")
            Text("경도: ${currentLongitude ?: "정보 없음"}")
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
                        .heightIn(max = 200.dp) // Limit height to avoid overflowing
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

        // Actual Travel Time Input Section
        Text("실제 이동 시간 (분):", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = actualTravelTimeInput,
            onValueChange = { newValue ->
                actualTravelTimeInput = newValue.filter { it.isDigit() }
            },
            label = { Text("실제 이동 시간 입력") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("입력 값: ${actualTravelTimeMinutes ?: "정보 없음"}", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Save to Firebase Button
        Button(
            onClick = {
                if (selectedEvent == null || originLatLng == null || destinationLatLng == null || actualTravelTimeMinutes == null || estimatedTravelTimeMinutes == null) {
                    Toast.makeText(context, "모든 정보를 입력하고 이벤트를 선택해주세요.", Toast.LENGTH_SHORT).show()
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
                            actualEtaMin = actualTravelTimeMinutes,
                            weather = currentWeather,
                            hourOfDay = SimpleDateFormat("HH", Locale.getDefault()).format(eventDate).toInt(),
                            dayOfWeek = SimpleDateFormat("u", Locale.getDefault()).format(eventDate).toInt(), // 1 for Monday, 7 for Sunday
                            distanceKm = 0.0, // This needs to be calculated from LocationRepository if available
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
            modifier = Modifier.fillMaxWidth()
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