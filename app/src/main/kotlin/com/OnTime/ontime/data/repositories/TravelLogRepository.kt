package com.OnTime.ontime.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.OnTime.ontime.data.models.TravelLog
import kotlinx.coroutines.tasks.await

class TravelLogRepository(private val firestore: FirebaseFirestore) {

    suspend fun saveTravelLog(travelLog: TravelLog) {
        try {
            firestore.collection("travel_logs")
                .add(travelLog)
                .await() // Await the completion of the add operation
            println("TravelLog saved successfully: $travelLog")
        } catch (e: Exception) {
            println("Error saving TravelLog: ${e.message}")
            throw e
        }
    }
}
