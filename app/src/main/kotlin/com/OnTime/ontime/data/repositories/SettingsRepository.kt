package com.OnTime.ontime.data.repositories

import android.content.Context
import com.OnTime.ontime.data.models.UserPreferences
import com.OnTime.ontime.data.models.CustomLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.OnTime.ontime.util.Logger
import kotlinx.coroutines.tasks.await

class SettingsRepository(private val context: Context) { // Context might still be needed for other things, keep it for now.

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    sealed class SaveResult {
        object Success : SaveResult()
        object UserNotLoggedIn : SaveResult()
        data class FirestoreError(val message: String?) : SaveResult()
    }

    suspend fun getUserPreferences(): UserPreferences {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Logger.d("No user logged in. Returning default UserPreferences.")
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
            Logger.e("Error fetching user preferences from Firestore", e)
            UserPreferences() // Return default preferences on error
        }
    }

    suspend fun saveUserPreferences(preferences: UserPreferences) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Logger.d("No user logged in. User preferences not saved to Firestore.")
            return
        }

        try {
            usersCollection.document(currentUser.uid)
                .collection("preferences")
                .document("userPreferences")
                .set(preferences, SetOptions.merge())
                .await()
            Logger.d("User preferences saved to Firestore successfully for user: ${currentUser.uid}")
        } catch (e: Exception) {
            Logger.e("Error saving user preferences to Firestore", e)
            throw e // Re-throw to propagate the error
        }
    }

    suspend fun saveCustomLocation(location: CustomLocation): SaveResult {
        Logger.d("Attempting to save custom location: ${location.name}")
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Logger.d("Save failed: No user logged in.")
            return SaveResult.UserNotLoggedIn
        }
        Logger.d("User is logged in with UID: ${currentUser.uid}")

        return try {
            val documentRef = if (location.id.isEmpty()) {
                Logger.d("Location has no ID, creating new document.")
                usersCollection.document(currentUser.uid)
                    .collection("customLocations")
                    .document() // Auto-generate ID
            } else {
                Logger.d("Location has ID '${location.id}', updating existing document.")
                usersCollection.document(currentUser.uid)
                    .collection("customLocations")
                    .document(location.id)
            }
            val locationToSave = if (location.id.isEmpty()) {
                location.copy(id = documentRef.id) // Update ID in the object
            } else {
                location
            }

            Logger.d("Saving to Firestore: $locationToSave")
            documentRef.set(locationToSave, SetOptions.merge())
                .await()
            Logger.i("Custom location saved successfully to document ${documentRef.id} for user: ${currentUser.uid}")
            SaveResult.Success
        } catch (e: Exception) {
            Logger.e("Error saving custom location to Firestore", e)
            SaveResult.FirestoreError(e.message)
        }
    }

    suspend fun getCustomLocations(): List<CustomLocation> {
        Logger.d("Attempting to fetch custom locations.")
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Logger.d("Fetch failed: No user logged in. Returning empty list.")
            return emptyList()
        }
        Logger.d("Fetching locations for user UID: ${currentUser.uid}")

        return try {
            val querySnapshot = usersCollection.document(currentUser.uid)
                .collection("customLocations")
                .get()
                .await()
            val locations = querySnapshot.documents.mapNotNull { it.toObject(CustomLocation::class.java) }
            Logger.i("Successfully fetched ${locations.size} custom locations from Firestore.")
            locations
        } catch (e: Exception) {
            Logger.e("Error fetching custom locations from Firestore", e)
            emptyList() // Return empty list on error
        }
    }

    suspend fun deleteCustomLocation(locationId: String): SaveResult {
        Logger.d("Attempting to delete custom location with ID: $locationId")
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Logger.d("Delete failed: No user logged in.")
            return SaveResult.UserNotLoggedIn
        }

        return try {
            usersCollection.document(currentUser.uid)
                .collection("customLocations")
                .document(locationId)
                .delete()
                .await()
            Logger.i("Successfully deleted custom location $locationId for user ${currentUser.uid}")
            SaveResult.Success
        } catch (e: Exception) {
            Logger.e("Error deleting custom location $locationId from Firestore", e)
            SaveResult.FirestoreError(e.message)
        }
    }
}