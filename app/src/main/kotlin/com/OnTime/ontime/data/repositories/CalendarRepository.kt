package com.OnTime.ontime.data.repositories

import android.content.Context
import android.util.Log
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
import com.OnTime.ontime.util.LocationConverter

// 클래스 생성 시 context를 받습니다.
class CalendarRepository(private val context: Context) {

    private var cachedService: Calendar? = null
    private var lastAccountEmail: String? = null

    // ★★★ [수정] getEvents 메서드에서 불필요한 context 파라미터를 제거합니다. ★★★
    suspend fun getEvents(maxResults: Int, pageToken: String? = null): Pair<List<CalendarEvent>, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    return@withContext Pair(emptyList(), null)
                }

                val service = getOrCreateService(account.email)

                val threeHoursAgo = DateTime(System.currentTimeMillis() - 3 * 60 * 60 * 1000)

                val eventsRequest = service.events().list("primary")
                    .setMaxResults(maxResults)
                    .setTimeMin(threeHoursAgo)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setFields("nextPageToken,items(id,summary,location,start,end,description)")

                pageToken?.let {
                    eventsRequest.setPageToken(it)
                }

                val events = eventsRequest.execute()

                if (events.items.isNullOrEmpty()) {
                    return@withContext Pair(emptyList(), events.nextPageToken)
                }

                val calendarEvents = events.items.mapNotNull { event ->
                    val startTime = event.start?.dateTime?.value ?: event.start?.date?.value
                    val endTime = event.end?.dateTime?.value ?: event.end?.date?.value

                    if (startTime != null && endTime != null) {
                        val destinationLatLng = event.location?.let {
                            LocationConverter.addressToLatLng(context, it)
                        }

                        CalendarEvent(
                            id = event.id ?: "",
                            title = event.summary ?: "제목 없음",
                            location = event.location,
                            startTime = startTime,
                            endTime = endTime,
                            description = event.description,
                            destinationLatLng = destinationLatLng
                        )
                    } else null
                }
                Pair(calendarEvents, events.nextPageToken)

            } catch (e: Exception) {
                Log.e("OnTime", "캘린더 로딩 오류: ${e.message}")
                Pair(emptyList(), null)
            }
        }
    }

    // ★★★ [수정] getOrCreateService 메서드에서도 불필요한 context 파라미터를 제거합니다. ★★★
    private fun getOrCreateService(email: String?): Calendar {
        if (cachedService != null && lastAccountEmail == email) {
            return cachedService!!
        }

        // 클래스 멤버 context를 사용합니다.
        val account = GoogleSignIn.getLastSignedInAccount(context)!!
        val credential = GoogleAccountCredential.usingOAuth2(
            context, // 클래스 멤버 context
            setOf(CalendarScopes.CALENDAR_READONLY)
        ).setSelectedAccount(account.account)

        cachedService = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("OnTime").build()

        lastAccountEmail = email
        return cachedService!!
    }

    suspend fun getEventById(eventId: String): CalendarEvent? {
        return null // 이 부분은 구현되지 않았으므로 그대로 둡니다.
    }
}
