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

class CalendarRepository {
    
    // 캐시된 서비스 인스턴스 (재사용으로 속도 향상)
    private var cachedService: Calendar? = null
    private var lastAccountEmail: String? = null

    suspend fun getEvents(context: Context): List<CalendarEvent> {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    return@withContext emptyList()
                }

                // 서비스 재사용으로 초기화 시간 단축
                val service = getOrCreateService(context, account.email)
                
                val now = DateTime(System.currentTimeMillis())
                val oneWeekLater = DateTime(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)
                
                // 최적화된 API 호출
                val events = service.events().list("primary")
                    .setMaxResults(20)
                    .setTimeMin(now)
                    .setTimeMax(oneWeekLater) // 범위 제한으로 속도 향상
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setFields("items(id,summary,location,start,end,description)") // 필요한 필드만
                    .execute()

                if (events.items.isNullOrEmpty()) {
                    return@withContext emptyList()
                }

                // 빠른 변환
                events.items.mapNotNull { event ->
                    val startTime = event.start?.dateTime?.value ?: event.start?.date?.value
                    val endTime = event.end?.dateTime?.value ?: event.end?.date?.value
                    
                    if (startTime != null && endTime != null) {
                        CalendarEvent(
                            id = event.id ?: "",
                            title = event.summary ?: "제목 없음",
                            location = event.location,
                            startTime = startTime,
                            endTime = endTime,
                            description = event.description
                        )
                    } else null
                }

            } catch (e: Exception) {
                Log.e("OnTime", "캘린더 로딩 오류: ${e.message}")
                emptyList()
            }
        }
    }
    
    private fun getOrCreateService(context: Context, email: String?): Calendar {
        // 같은 계정이면 캐시된 서비스 재사용
        if (cachedService != null && lastAccountEmail == email) {
            return cachedService!!
        }
        
        val account = GoogleSignIn.getLastSignedInAccount(context)!!
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
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
        return null
    }
}
