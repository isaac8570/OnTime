package com.OnTime.ontime.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // TODO: Implement reminder logic
        // "출발했는지 묻는 반복" 알림 처리
        
        return Result.success()
    }
}
