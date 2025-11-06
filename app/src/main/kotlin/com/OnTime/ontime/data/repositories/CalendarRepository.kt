package com.OnTime.ontime.data.repositories

import android.content.Context
import com.OnTime.ontime.data.models.CalendarEvent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class CalendarRepository {

    suspend fun getEvents(context: Context): List<CalendarEvent> {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    // Not signed in, return empty list
                    return@withContext emptyList()
                }

                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    setOf(CalendarScopes.CALENDAR_READONLY)
                ).setSelectedAccount(account.account)

                val transport = NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()
                val service = Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("OnTime")
                    .build()

                val now = DateTime(System.currentTimeMillis())
                val events = service.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                events.items.map { event ->
                    CalendarEvent(
                        id = event.id,
                        title = event.summary,
                        location = event.location,
                        startTime = event.start.dateTime?.value ?: event.start.date.value,
                        endTime = event.end.dateTime?.value ?: event.end.date.value,
                        description = event.description
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getEventById(eventId: String): CalendarEvent? {
        // TODO: Implement event retrieval
        return null
    }
}
