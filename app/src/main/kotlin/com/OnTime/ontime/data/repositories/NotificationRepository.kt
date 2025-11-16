package com.OnTime.ontime.data.repositories

import com.OnTime.ontime.data.models.NotificationRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationsCollection = db.collection("notification_records")

    suspend fun saveNotificationRecord(record: NotificationRecord) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("Warning: No user logged in. Notification record not saved to Firestore.")
            return
        }

        // Add user ID to the record
        val recordWithUserId = record.copy(userId = currentUser.uid)

        try {
            // Firebase will auto-generate document ID
            notificationsCollection.document(currentUser.uid)
                .collection("user_notifications")
                .add(recordWithUserId)
                .await()
            println("Notification record saved to Firestore successfully for user: ${currentUser.uid}")
        } catch (e: Exception) {
            println("Error saving notification record to Firestore: ${e.message}")
            throw e
        }
    }

    suspend fun getNotificationRecords(): List<NotificationRecord> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("Warning: No user logged in. Returning empty list of notification records.")
            return emptyList()
        }

        return try {
            val querySnapshot = notificationsCollection.document(currentUser.uid)
                .collection("user_notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Order by newest first
                .get()
                .await()
            querySnapshot.documents.mapNotNull { it.toObject(NotificationRecord::class.java) }
        } catch (e: Exception) {
            println("Error fetching notification records from Firestore: ${e.message}")
            emptyList()
        }
    }
}
