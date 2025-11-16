package com.OnTime.ontime

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.OnTime.ontime.data.models.TravelLog
import com.OnTime.ontime.data.repositories.TravelDataRepository
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class DataCollectionActivity : AppCompatActivity() {

    private lateinit var travelDataRepository: TravelDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_collection)

        travelDataRepository = TravelDataRepository()

        val etUserId: EditText = findViewById(R.id.etUserId)
        val etGoogleEtaMin: EditText = findViewById(R.id.etGoogleEtaMin)
        val etActualEtaMin: EditText = findViewById(R.id.etActualEtaMin)
        val etWeather: EditText = findViewById(R.id.etWeather)
        val etHourOfDay: EditText = findViewById(R.id.etHourOfDay)
        val etDayOfWeek: EditText = findViewById(R.id.etDayOfWeek)
        val etDistanceKm: EditText = findViewById(R.id.etDistanceKm)
        val btnSubmitData: Button = findViewById(R.id.btnSubmitData)

        btnSubmitData.setOnClickListener {
            val userId = etUserId.text.toString().trim()
            val googleEtaMin = etGoogleEtaMin.text.toString().trim().toIntOrNull()
            val actualEtaMin = etActualEtaMin.text.toString().trim().toIntOrNull()
            val weather = etWeather.text.toString().trim()
            val hourOfDay = etHourOfDay.text.toString().trim().toIntOrNull()
            val dayOfWeek = etDayOfWeek.text.toString().trim().toIntOrNull()
            val distanceKm = etDistanceKm.text.toString().trim().toDoubleOrNull()

            if (userId.isEmpty() || googleEtaMin == null || actualEtaMin == null || weather.isEmpty() ||
                hourOfDay == null || dayOfWeek == null || distanceKm == null) {
                Toast.makeText(this, "모든 필드를 채워주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (googleEtaMin <= 0 || actualEtaMin <= 0 || hourOfDay < 0 || hourOfDay > 23 ||
                dayOfWeek < 1 || dayOfWeek > 7 || distanceKm <= 0) {
                Toast.makeText(this, "유효한 값을 입력해주세요 (0보다 큰 값, 시간 0-23, 요일 1-7).", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val travelLog = TravelLog(
                userId = userId,
                googleEtaMin = googleEtaMin,
                actualEtaMin = actualEtaMin,
                weather = weather,
                hourOfDay = hourOfDay,
                dayOfWeek = dayOfWeek,
                distanceKm = distanceKm,
                timestamp = Date() // Use current Date
            )

            lifecycleScope.launch {
                try {
                    travelDataRepository.saveTravelLog(travelLog)
                    Toast.makeText(this@DataCollectionActivity, "데이터가 성공적으로 제출되었습니다!", Toast.LENGTH_SHORT).show()
                    etUserId.text.clear()
                    etGoogleEtaMin.text.clear()
                    etActualEtaMin.text.clear()
                    etWeather.text.clear()
                    etHourOfDay.text.clear()
                    etDayOfWeek.text.clear()
                    etDistanceKm.text.clear()
                } catch (e: Exception) {
                    Toast.makeText(this@DataCollectionActivity, "데이터 제출 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("DataCollectionActivity", "Error adding document", e)
                }
            }
        }
    }
}
