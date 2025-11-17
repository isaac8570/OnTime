package com.OnTime.ontime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import android.app.Application
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.OnTime.ontime.data.models.CustomLocation
import com.OnTime.ontime.data.repositories.SettingsRepository
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.ui.viewmodel.CustomLocationViewModel
import com.OnTime.ontime.ui.viewmodel.ViewModelFactory

class CustomLocationSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnTimeTheme {
                val settingsRepository = SettingsRepository(application)
                val factory = CustomLocationViewModelFactory(application, settingsRepository)
                val customLocationViewModel: CustomLocationViewModel = viewModel(factory = factory)

                CustomLocationSettingsScreen(
                    onBackClick = { finish() },
                    customLocationViewModel = customLocationViewModel
                )
            }
        }
    }

    class CustomLocationViewModelFactory(
        private val application: Application,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomLocationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomLocationViewModel(application, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLocationSettingsScreen(
    onBackClick: () -> Unit,
    customLocationViewModel: CustomLocationViewModel
) {
    val customLocations by customLocationViewModel.customLocations.observeAsState(emptyList())
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("사용자 정의 위치 설정") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "뒤로 가기")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, "새 위치 추가")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(customLocations) { location ->
                CustomLocationItem(location = location)
            }
        }

        if (showDialog) {
            AddCustomLocationDialog(
                onDismiss = { showDialog = false },
                onSave = { name, address, latitude, longitude ->
                    customLocationViewModel.saveCustomLocation(
                        CustomLocation(name = name, address = address, latitude = latitude, longitude = longitude)
                    )
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun CustomLocationItem(location: CustomLocation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = location.name, style = MaterialTheme.typography.titleMedium)
            Text(text = location.address, style = MaterialTheme.typography.bodyMedium)
            Text(text = "위도: ${location.latitude}, 경도: ${location.longitude}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AddCustomLocationDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, latitude: Double, longitude: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 사용자 정의 위치 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름 (예: 집, 회사)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("주소") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("위도") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("경도") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        address,
                        latitude.toDoubleOrNull() ?: 0.0,
                        longitude.toDoubleOrNull() ?: 0.0
                    )
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
