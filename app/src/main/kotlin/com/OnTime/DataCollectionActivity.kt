package com.OnTime.ontime

import com.OnTime.ontime.R // 이 import는 그대로 둡니다.
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// import com.google.firebase.FirebaseApp // 이 줄은 주석 처리합니다.
// import com.google.firebase.firestore.FirebaseFirestore // 이 줄은 주석 처리합니다.
import java.text.SimpleDateFormat // 이 줄은 그대로 둡니다 (SimpleDateFormat은 Locale과 Date를 필요로 하므로 일단 둡니다).
import java.util.Date // 이 줄은 그대로 둡니다.
import java.util.Locale // 이 줄은 그대로 둡니다.

class DataCollectionActivity : AppCompatActivity() {

    // private lateinit var firestore: FirebaseFirestore // 이 줄은 주석 처리합니다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_collection) // 이 줄은 그대로 둡니다.

        // 아래의 모든 Firebase 및 UI 로직을 임시로 주석 처리합니다.
        /*
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        firestore = FirebaseFirestore.getInstance()

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

            val travelLog = hashMapOf(
                "user_id" to userId,
                "google_eta_min" to googleEtaMin,
                "actual_eta_min" to actualEtaMin,
                "weather" to weather,
                "hour_of_day" to hourOfDay,
                "day_of_week" to dayOfWeek,
                "distance_km" to distanceKm,
                "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            firestore.collection("travel_logs")
                .add(travelLog)
                .addOnSuccessListener {
                    Toast.makeText(this, "데이터가 성공적으로 제출되었습니다!", Toast.LENGTH_SHORT).show()
                    etUserId.text.clear()
                    etGoogleEtaMin.text.clear()
                    etActualEtaMin.text.clear()
                    etWeather.text.clear()
                    etHourOfDay.text.clear()
                    etDayOfWeek.text.clear()
                    etDistanceKm.text.clear()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "데이터 제출 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("DataCollectionActivity", "Error adding document", e)
                }
        }
        */
        // 액티비티가 시작되면 토스트 메시지를 띄워봅니다.
        Toast.makeText(this, "DataCollectionActivity 시작됨", Toast.LENGTH_SHORT).show()
    }
}
