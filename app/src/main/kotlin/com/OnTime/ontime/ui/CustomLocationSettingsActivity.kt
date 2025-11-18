package com.OnTime.ontime.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.OnTime.ontime.data.models.CustomLocation
import com.OnTime.ontime.data.repositories.SettingsRepository
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.ui.viewmodel.CustomLocationViewModel

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
    val context = LocalContext.current
    val customLocations by customLocationViewModel.customLocations.observeAsState(emptyList())
    val saveError by customLocationViewModel.saveError.observeAsState()

    // Show a toast on save error
    LaunchedEffect(saveError) {
        saveError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            customLocationViewModel.onSaveErrorShown()
        }
    }

    val addLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val name = data?.getStringExtra("name")
            val address = data?.getStringExtra("address")
            val latitude = data?.getDoubleExtra("latitude", 0.0)
            val longitude = data?.getDoubleExtra("longitude", 0.0)

            if (name != null && address != null) {
                customLocationViewModel.saveCustomLocation(
                    CustomLocation(
                        name = name,
                        address = address,
                        latitude = latitude ?: 0.0,
                        longitude = longitude ?: 0.0
                    )
                )
            }
        }
    }

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
            FloatingActionButton(onClick = {
                val intent = Intent(context, AddCustomLocationMapActivity::class.java)
                addLocationLauncher.launch(intent)
            }) {
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
                CustomLocationItem(
                    location = location,
                    onDeleteClick = { customLocationViewModel.deleteCustomLocation(location.id) }
                )
            }
        }
    }
}

@Composable
fun CustomLocationItem(location: CustomLocation, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = location.name, style = MaterialTheme.typography.titleMedium)
                Text(text = location.address, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = "삭제")
            }
        }
    }
}
