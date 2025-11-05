package com.OnTime.ontime.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.OnTime.ontime.R
import com.OnTime.ontime.service.RouteCalculationWorker
import kotlinx.coroutines.launch

class TestGeminiActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 간단한 테스트 UI
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val textView = TextView(this).apply {
            text = "Gemini AI 알림 테스트"
            textSize = 20f
        }
        
        val button = Button(this).apply {
            text = "알림 생성 테스트"
            setOnClickListener {
                testGeminiNotification()
            }
        }
        
        layout.addView(textView)
        layout.addView(button)
        setContentView(layout)
    }
    
    private fun testGeminiNotification() {
        val workRequest = OneTimeWorkRequestBuilder<RouteCalculationWorker>()
            .setInputData(
                workDataOf(
                    "destination" to "강남역",
                    "userTone" to "감성적이고 설렘을 유도하는"
                )
            )
            .build()
        
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
