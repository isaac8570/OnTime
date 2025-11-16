package com.OnTime.ontime.data.repositories

import com.OnTime.ontime.data.models.TravelLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TravelDataRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val travelLogsCollection = db.collection("travel_logs")

    suspend fun saveTravelLog(travelLog: TravelLog) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("Warning: No user logged in. Travel log not saved to Firestore.")
            return
        }

        try {
            // Store travel logs under a subcollection for the specific user
            travelLogsCollection.document(currentUser.uid)
                .collection("user_travel_logs")
                .add(travelLog)
                .await()
            println("Travel log saved to Firestore successfully for user: ${currentUser.uid}")
        } catch (e: Exception) {
            println("Error saving travel log to Firestore: ${e.message}")
            throw e
        }
    }

    // Optional: Add a function to retrieve travel logs if needed later
    suspend fun getTravelLogs(): List<TravelLog> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("Warning: No user logged in. Returning empty list of travel logs.")
            return emptyList()
        }

        return try {
            val querySnapshot = travelLogsCollection.document(currentUser.uid)
                .collection("user_travel_logs")
                .get()
                .await()
            querySnapshot.documents.mapNotNull { it.toObject(TravelLog::class.java) }
        } catch (e: Exception) {
            println("Error fetching travel logs from Firestore: ${e.message}")
            emptyList()
        }
    }
}
