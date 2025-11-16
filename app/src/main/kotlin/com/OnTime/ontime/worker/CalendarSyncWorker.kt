package com.OnTime.ontime.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.TravelDataRepository // Corrected import
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.manager.TravelMonitorManager
import com.OnTime.ontime.util.LocationConverter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val firestore = FirebaseFirestore.getInstance() // Get Firestore instance
            val weatherRepository = WeatherRepository() // Instantiate WeatherRepository
            val locationRepository = LocationRepository(applicationContext) // Instantiate LocationRepository
            val calendarRepository = CalendarRepository(applicationContext)
            // val travelLogRepository = TravelLogRepository(firestore) // Original - REMOVED
            val travelDataRepository = TravelDataRepository() // Corrected instantiation

            val travelMonitorManager = TravelMonitorManager.getInstance(
                applicationContext,
                calendarRepository,
                locationRepository,
                weatherRepository,
                // travelLogRepository // Original - REMOVED
                travelDataRepository // Corrected
            )

            // ★★★ [수정] getEvents() 호출 시 불필요한 인자를 제거합니다. ★★★
            val events = calendarRepository.getEvents()

            if (events.isNotEmpty()) {
                travelMonitorManager.onCalendarEventsFetched(events)
                Result.success()
            } else {
                println("No calendar events fetched.")
                Result.failure()
            }
        }
    }
}