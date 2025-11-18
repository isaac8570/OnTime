package com.OnTime.ontime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.OnTime.ontime.ui.theme.OnTimeTheme
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.OnTime.ontime.NotificationBuilder
import com.OnTime.ontime.data.repositories.SettingsRepository
import com.OnTime.ontime.service.NotificationScheduler
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.ui.viewmodel.AlarmTestViewModel

class AlarmTestActivity : ComponentActivity() {

    private val factory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val notificationScheduler = NotificationScheduler(
                    context = applicationContext,
                    notificationBuilder = NotificationBuilder(),
                    settingsRepository = SettingsRepository(application)
                )
                return AlarmTestViewModel(notificationScheduler) as T
            }
        }
    }

    private val viewModel: AlarmTestViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            // Notification permission launcher
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                // Handle permission result if needed
            }

            // Request notification permission on launch if not granted
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            OnTimeTheme {
                AlarmTestScreen(onSendTestNotification = {
                    Toast.makeText(context, "테스트 알림을 요청했습니다. 곧 도착합니다.", Toast.LENGTH_SHORT).show()
                    viewModel.sendTestNotification()
                })
            }
        }
    }
}

@Composable
fun AlarmTestScreen(onSendTestNotification: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onSendTestNotification) {
            Text("테스트 알림 보내기")
        }
    }
}
