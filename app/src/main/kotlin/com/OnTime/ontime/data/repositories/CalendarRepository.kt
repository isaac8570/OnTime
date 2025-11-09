package com.OnTime.ontime.data.repositories

import android.content.Context
import android.util.Log // Log를 사용하기 위해 꼭 import 해야 합니다.
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
        // 1. 함수가 호출되었는지 확인
        Log.d("OnTime-API", "➡️ getEvents() 함수 시작")

        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    // 로그인 계정이 없는 경우
                    Log.w("OnTime-API", "🚨 2단계 실패: 구글 로그인 계정을 찾을 수 없음")
                    return@withContext emptyList()
                }
                Log.d("OnTime-API", "✅ 2단계 성공: 구글 계정 찾음 (${account.email})")

                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    setOf(CalendarScopes.CALENDAR_READONLY)
                ).setSelectedAccount(account.account)

                val transport = NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()
                val service = Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("OnTime")
                    .build()

                Log.d("OnTime-API", "✅ 3단계 성공: Calendar API 서비스 생성 완료. 이제 API 호출 시작...")

                val now = DateTime(System.currentTimeMillis())
                val events = service.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                Log.d("OnTime-API", "✅ 4단계 성공: API 호출 완료!")

                if (events.items == null || events.items.isEmpty()) {
                    Log.d("OnTime-API", "ℹ️ 5단계 정보: API 응답은 성공했으나, 가져올 일정이 없음 (events.items가 비어있음)")
                    return@withContext emptyList()
                }

                Log.d("OnTime-API", "✅ 5단계 성공: 가져온 원본 일정 개수: ${events.items.size}")

                val eventList = events.items.mapNotNull { event ->
                    try {
                        // 하루 종일 일정과 시간 지정 일정을 모두 안전하게 처리
                        val startTime = event.start?.dateTime?.value ?: event.start?.date?.value
                        val endTime = event.end?.dateTime?.value ?: event.end?.date?.value

                        // 시작 또는 종료 시간이 없으면 유효하지 않은 이벤트로 간주하고 건너뜀
                        if (startTime == null || endTime == null) return@mapNotNull null

                        CalendarEvent(
                            id = event.id,
                            title = event.summary ?: "제목 없음", // 제목이 null인 경우를 대비
                            location = event.location,
                            startTime = startTime,
                            endTime = endTime,
                            description = event.description
                        )
                    } catch (e: Exception) {
                        Log.e("OnTime-API", "⚠️ 6단계 변환 오류: 특정 이벤트를 변환하다 실패함", e)
                        null // 오류 발생 시 해당 이벤트는 제외하고 계속 진행
                    }
                }
                Log.d("OnTime-API", "✅ 6단계 성공: 최종 변환된 일정 개수: ${eventList.size}")
                eventList

            } catch (e: Exception) {
                // API 호출 자체 또는 그 외 과정에서 오류가 발생한 경우
                Log.e("OnTime-API", "🔥🔥🔥 API 호출 또는 처리 중 심각한 오류 발생 🔥🔥🔥", e)
                emptyList()
            }
        }
    }

    suspend fun getEventById(eventId: String): CalendarEvent? {
        // TODO: Implement event retrieval
        return null
    }
}
