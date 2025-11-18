package com.OnTime.ontime.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.util.LocationConverter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

class AddCustomLocationMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY")
            if (apiKey == null || apiKey.startsWith("\${'$'}{")) {
                Log.e("PlacesApi", "API key not found or not resolved in manifest")
                Toast.makeText(this, "Maps API 키를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("PlacesApi", "Failed to load meta-data", e)
            Toast.makeText(this, "Maps API 설정에 실패했습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }


        setContent {
            OnTimeTheme {
                AddLocationMapScreen(
                    onSave = { name, address, latitude, longitude ->
                        val resultIntent = Intent().apply {
                            putExtra("name", name)
                            putExtra("address", address)
                            putExtra("latitude", latitude)
                            putExtra("longitude", longitude)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddLocationMapScreen(
    onSave: (name: String, address: String, latitude: Double, longitude: Double) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val placesClient = Places.createClient(context)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var keyword by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }

    var searchResults by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    val defaultLatLng = LatLng(37.5665, 126.9780) // Seoul
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 15f)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted, get location
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val currentLatLng = LatLng(it.latitude, it.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e("Location", "Failed to get location", e)
                }
            }
        }
    )

    // Get current location on launch
    LaunchedEffect(Unit) {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val currentLatLng = LatLng(it.latitude, it.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e("Location", "Failed to get location", e)
                }
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }


    // Update camera when selectedLatLng changes
    LaunchedEffect(selectedLatLng) {
        selectedLatLng?.let { latLng ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
        }
    }

    // Geocode the location when the user stops moving the map
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val currentLatLng = cameraPositionState.position.target
            // To avoid constant geocoding, we only do it if there's no selected address yet,
            // or if the user moves the map significantly.
            // For this implementation, we will update the selected location by long-pressing.
        }
    }

    // Perform autocomplete search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(searchQuery)
                .build()
            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                searchResults = response.autocompletePredictions
            }.addOnFailureListener { exception ->
                Log.e("PlacesApi", "Error getting autocomplete predictions", exception)
            }
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("사용자 정의 위치 추가") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            // Google Map takes up the whole screen
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLongClick = { latLng ->
                    selectedLatLng = latLng
                    coroutineScope.launch {
                        val address = LocationConverter.latLngToAddress(context, latLng.latitude, latLng.longitude)
                        selectedAddress = address
                    }
                },
                onMapLoaded = {
                    Log.d("GoogleMap", "Google Map loaded successfully!")
                }
            ) {
                // Add a marker to the selected location
                selectedLatLng?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = selectedAddress ?: "선택된 위치"
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Search bars and save button at the top
                Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 4.dp) {
                    Column {
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text("장소 이름 (예: 집, 회사, 헬스장)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("주소 검색") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                            })
                        )
                    }
                }

                // Search results
                if (searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(searchResults) { prediction ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable {
                                        val placeId = prediction.placeId
                                        val placeFields =
                                            listOf(Place.Field.LAT_LNG, Place.Field.ADDRESS)
                                        val request =
                                            FetchPlaceRequest.newInstance(placeId, placeFields)
                                        placesClient
                                            .fetchPlace(request)
                                            .addOnSuccessListener { response ->
                                                val place = response.place
                                                selectedLatLng = place.latLng
                                                selectedAddress = place.address
                                                searchQuery = ""
                                                searchResults = emptyList()
                                                keyboardController?.hide()
                                            }
                                    }
                            ) {
                                Text(
                                    text = prediction.getFullText(null).toString(),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom sheet with selected address and save button
                if (selectedLatLng != null) {
                    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "선택된 주소: ${selectedAddress ?: "주소를 불러오는 중..."}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (keyword.isNotBlank() && selectedAddress != null && selectedLatLng != null) {
                                        onSave(keyword, selectedAddress!!, selectedLatLng!!.latitude, selectedLatLng!!.longitude)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = keyword.isNotBlank() && selectedAddress != null
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("이 위치로 저장")
                            }
                        }
                    }
                }
            }
        }
    }
}