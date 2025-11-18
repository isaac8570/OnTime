package com.OnTime.ontime.data.repositories

import android.content.Context
import com.OnTime.ontime.data.models.UserPreferences
import com.OnTime.ontime.data.models.CustomLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SettingsRepository(private val context: Context) { // Context might still be needed for other things, keep it for now.

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun getUserPreferences(): UserPreferences {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("Warning: No user logged in. Returning default UserPreferences.")
            return UserPreferences()
        }

        return try {
            val document = usersCollection.document(currentUser.uid)
                .collection("preferences")
                .document("userPreferences")
                .get()
                .await()
            document.toObject(UserPreferences::class.java) ?: UserPreferences()
        } catch (e: Exception) {
            println("Error fetching user preferences from Firestore: ${e.message}")
            UserPreferences() // Return default preferences on error
        }
    }

    suspend fun saveUserPreferences(preferences: UserPreferences) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("Warning: No user logged in. User preferences not saved to Firestore.")
            return
        }

        try {
            usersCollection.document(currentUser.uid)
                .collection("preferences")
                .document("userPreferences")
                .set(preferences, SetOptions.merge())
                .await()
            println("User preferences saved to Firestore successfully for user: ${currentUser.uid}")
        } catch (e: Exception) {
            println("Error saving user preferences to Firestore: ${e.message}")
            throw e // Re-throw to propagate the error
        }
    }

    suspend fun saveCustomLocation(location: CustomLocation) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("Warning: No user logged in. Custom location not saved to Firestore.")
            return
        }

        try {
            val documentRef = if (location.id.isEmpty()) {
                usersCollection.document(currentUser.uid)
                    .collection("customLocations")
                    .document() // Auto-generate ID
            } else {
                usersCollection.document(currentUser.uid)
                    .collection("customLocations")
                    .document(location.id)
            }
            val locationToSave = if (location.id.isEmpty()) {
                location.copy(id = documentRef.id) // Update ID in the object
            } else {
                location
            }

            documentRef.set(locationToSave, SetOptions.merge())
                .await()
            println("Custom location saved to Firestore successfully for user: ${currentUser.uid}, location: ${locationToSave.name}")
        } catch (e: Exception) {
            println("Error saving custom location to Firestore: ${e.message}")
            throw e
        }
    }

    suspend fun getCustomLocations(): List<CustomLocation> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("Warning: No user logged in. Returning empty list of custom locations.")
            return emptyList()
        }

        return try {
            val querySnapshot = usersCollection.document(currentUser.uid)
                .collection("customLocations")
                .get()
                .await()
            querySnapshot.documents.mapNotNull { it.toObject(CustomLocation::class.java) }
        } catch (e: Exception) {
            println("Error fetching custom locations from Firestore: ${e.message}")
            emptyList() // Return empty list on error
        }
    }
}